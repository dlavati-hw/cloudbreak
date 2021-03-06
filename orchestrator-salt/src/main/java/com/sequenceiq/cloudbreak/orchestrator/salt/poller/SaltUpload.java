package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import java.io.IOException;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;

public class SaltUpload extends SaltFileUpload {

    private final String path;

    private final String fileName;

    private final byte[] content;

    public SaltUpload(SaltConnector sc, Set<String> targets, String path, String fileName, byte[] content) {
        super(sc, targets);
        this.path = path;
        this.fileName = fileName;
        this.content = content;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SaltUpload{");
        sb.append("sc=").append(getSaltConnector());
        sb.append(", originalTargets=").append(getOriginalTargets());
        sb.append(", path='").append(path).append('\'');
        sb.append(", fileName='").append(fileName).append('\'');
        sb.append(", targets=").append(getTargets());
        sb.append('}');
        return sb.toString();
    }

    GenericResponses upload() throws IOException {
        return getSaltConnector().upload(getTargets(), path, fileName, content);
    }
}
