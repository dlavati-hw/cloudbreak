package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.common.api.type.LoadBalancerType;

public class CloudLoadBalancerMetadata {

    private final LoadBalancerType type;

    private final String cloudDns;

    private final String hostedZoneId;

    private final String ip;

    private final String name;

    public CloudLoadBalancerMetadata(LoadBalancerType type, String cloudDns, String hostedZoneId, String ip, String name) {
        this.type = type;
        this.cloudDns = cloudDns;
        this.hostedZoneId = hostedZoneId;
        this.ip = ip;
        this.name = name;
    }

    public LoadBalancerType getType() {
        return type;
    }

    public String getCloudDns() {
        return cloudDns;
    }

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    public String getIp() {
        return ip;
    }

    public String getName() {
        return name;
    }
}
