package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import java.util.Collections;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunnerInfo;

public class RunnerInfoConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunnerInfoConverter.class);

    private RunnerInfoConverter() {
    }

    static Optional<RunnerInfo> convertToRunnerInfo(String key, JsonNode runnerInfoNode) {
        if (!runnerInfoNode.isObject()) {
            LOGGER.debug("Cannot convert JSON to runner info Object as the value is not "
                            + "a JSON object {} for key '{}'", runnerInfoNode, key);
            return Optional.empty();
        }
        RunnerInfo runnerInfo = new RunnerInfo();
        runnerInfo.setStateId(key);
        JsonNode changes = runnerInfoNode.get("changes");
        runnerInfo.setChanges(
                changes == null || !changes.isObject()
                        ? Collections.emptyMap()
                        : new ObjectMapper()
                        .convertValue(changes, new TypeReference<>() {
                        }));
        runnerInfo.setComment(runnerInfoNode.get("comment").asText());
        double duration;
        try {
            String[] durationArray = String.valueOf(runnerInfoNode.get("duration").asText()).split(" ");
            duration = Double.parseDouble(durationArray[0]);
        } catch (NumberFormatException ignored) {
            duration = 0.0;
        }
        runnerInfo.setDuration(duration);
        if (runnerInfoNode.has("name")) {
            runnerInfo.setName(runnerInfoNode.get("name").asText());
        }
        runnerInfo.setResult(runnerInfoNode.get("result").asBoolean());
        String runNum = runnerInfoNode.get("__run_num__").asText();
        runnerInfo.setRunNum(Integer.parseInt(runNum));
        runnerInfo.setStartTime(runnerInfoNode.get("start_time").asText());
        return Optional.of(runnerInfo);
    }
}
