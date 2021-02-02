package com.sequenceiq.cloudbreak.cm;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cm.commands.SyncApiCommandPollerConfig;
import com.sequenceiq.cloudbreak.cm.commands.SyncApiCommandRetriever;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
public class ClouderaManagerSyncApiCommandIdProvider {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ClouderaManagerSyncApiCommandIdProvider.class);

    private final SyncApiCommandPollerConfig syncApiCommandPollerConfig;

    private final SyncApiCommandRetriever syncApiCommandRetriever;

    private final ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    private final ExecutorService executorService;

    public ClouderaManagerSyncApiCommandIdProvider(
            SyncApiCommandRetriever syncApiCommandRetriever,
            ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider,
            SyncApiCommandPollerConfig syncApiCommandPollerConfig,
            @Qualifier("cloudbreakListeningScheduledExecutorService")
                    ExecutorService executorService) {
        this.syncApiCommandRetriever = syncApiCommandRetriever;
        this.executorService = executorService;
        this.clouderaManagerPollingServiceProvider = clouderaManagerPollingServiceProvider;
        this.syncApiCommandPollerConfig = syncApiCommandPollerConfig;
    }

    /**
     * Obtain a sync API client config command ID with the following steps:
     * 1. check are there any currently running commands -> stop if there is
     * 2. check are there any existing successful sync API commands (that have finished already) - to collect wrong command IDs
     * 3. execute the sync API command with a timeout
     * 4. if the API command ties out, use 1. with polling
     * 5. if polling finished, do a last check, returns with the command id or throw an exception
     */
    public BigDecimal executeSyncApiCommandAndGetCommandId(String commandName, ClustersResourceApi api, Stack stack,
            List<ApiCommand> activeCommands, Callable<ApiCommand> callable) throws CloudbreakException, ApiException {
        Optional<BigDecimal> runningCommandIdOpt = getRunningCommandIdFromActiveCommands(commandName, activeCommands);
        if (runningCommandIdOpt.isPresent()) {
            LOGGER.debug("Found actively running {} command. Skip execution.", commandName);
            return runningCommandIdOpt.get();
        }
        Optional<BigDecimal> lastSyncApiCommandId = syncApiCommandRetriever.getLastFinishedCommandId(commandName, api, stack);
        if (lastSyncApiCommandId.isPresent()) {
            LOGGER.debug("Found already existing {} command with id: {}",
                    commandName, lastSyncApiCommandId);
        }
        Future<ApiCommand> future = executorService.submit(callable);
        try {
            ApiCommand command = future.get(syncApiCommandPollerConfig.getInterruptTimeoutSeconds(), TimeUnit.SECONDS);
            return Optional.ofNullable(command)
                    .map(ApiCommand::getId)
                    .orElse(null);
        } catch (TimeoutException timeoutException) {
            return getCommandIdAfterTimeout(commandName, api, stack,
                    future, lastSyncApiCommandId.orElse(null));
        } catch (ExecutionException ee) {
            future.cancel(true);
            throw new ApiException(ee.getCause());
        } catch (InterruptedException e) {
            future.cancel(true);
            throw new CloudbreakException(
                    String.format("Obtaining Cloudera Manager %s command ID interrupted", commandName), e);
        }
    }

    private Optional<BigDecimal> getRunningCommandIdFromActiveCommands(String commandName, List<ApiCommand> activeCommands) {
        Optional<BigDecimal> runningCommandIdOpt = Optional.empty();
        if (CollectionUtils.isEmpty(activeCommands)) {
            LOGGER.debug("Not found any active commands. Trigger {} command.", commandName);
        } else {
            BigDecimal runningCommandId = activeCommands
                    .stream()
                    .filter(c -> commandName.equals(c.getName()))
                    .findFirst()
                    .map(ApiCommand::getId)
                    .orElse(null);
            runningCommandIdOpt = Optional.ofNullable(runningCommandId);
        }
        return runningCommandIdOpt;
    }

    private BigDecimal getCommandIdAfterTimeout(String commandName, ClustersResourceApi api, Stack stack,
            Future<ApiCommand> future, BigDecimal lastSyncApiCommandId)
            throws ApiException, CloudbreakException {
        future.cancel(true);
        LOGGER.debug("{} command took too much time. Start command ID "
                + "query by listing active commands", commandName);
        clouderaManagerPollingServiceProvider.checkSyncApiCommandId(
                stack, api.getApiClient(), commandName, lastSyncApiCommandId,
                syncApiCommandRetriever);
        Optional<BigDecimal> finalCommandId = syncApiCommandRetriever.getCommandId(commandName, api, stack);
        if (finalCommandId.isEmpty()) {
            throw new CloudbreakException(
                    String.format("Obtaining Cloudera Manager %s command ID was not possible neither by"
                            + " listing Cloudera Manager commands nor using commandTable [stack: %s]", commandName, stack.getName()));
        }
        return finalCommandId.orElse(null);
    }
}