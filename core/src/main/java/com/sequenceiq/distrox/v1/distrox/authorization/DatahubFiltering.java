package com.sequenceiq.distrox.v1.distrox.authorization;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.authorization.resource.AuthorizationFiltering;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.distrox.v1.distrox.StackOperations;

@Component
public class DatahubFiltering implements AuthorizationFiltering<StackViewV4Responses> {

    public static final String ENV_NAME = "ENV_NAME";

    public static final String ENV_CRN = "ENV_CRN";

    @Inject
    private StackOperations stackOperations;

    @Inject
    private StackService stackService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Override
    public List<AuthorizationResource> getAllResources(Map<String, Object> args) {
        Optional<String> envCrn = resolveEnvCrn(args);
        Long workspaceId = workspaceService.getForCurrentUser().getId();
        if (envCrn.isPresent()) {
            return stackService.getAsAuthorizationResourcesByEnvCrn(workspaceId, envCrn.get(), StackType.WORKLOAD);
        } else {
            return stackService.getAsAuthorizationResources(workspaceId, StackType.WORKLOAD);
        }
    }

    @Override
    public StackViewV4Responses filterByIds(List<Long> authorizedResourceIds, Map<String, Object> args) {
        Optional<String> envCrn = resolveEnvCrn(args);
        Long workspaceId = workspaceService.getForCurrentUser().getId();
        return stackOperations.listByStackIds(workspaceId, authorizedResourceIds, envCrn.orElse(null), List.of(StackType.WORKLOAD));
    }

    @Override
    public StackViewV4Responses getAll(Map<String, Object> args) {
        List<StackType> stackTypes = List.of(StackType.WORKLOAD);
        String envName = getEnvName(args);
        return Strings.isNullOrEmpty(envName)
                ? stackOperations.listByEnvironmentCrn(workspaceService.getForCurrentUser().getId(), getEnvCrn(args), stackTypes)
                : stackOperations.listByEnvironmentName(workspaceService.getForCurrentUser().getId(), envName, stackTypes);
    }

    private Optional<String> resolveEnvCrn(Map<String, Object> args) {
        String envCrn = getEnvCrn(args);
        if (!Strings.isNullOrEmpty(envCrn)) {
            return Optional.of(envCrn);
        }
        String envName = getEnvName(args);
        if (!Strings.isNullOrEmpty(envName)) {
            return Optional.ofNullable(environmentClientService.getCrnByName(envName));
        }
        return Optional.empty();
    }

    private String getEnvName(Map<String, Object> args) {
        return (String) args.get(ENV_NAME);
    }

    private String getEnvCrn(Map<String, Object> args) {
        return (String) args.get(ENV_CRN);
    }
}
