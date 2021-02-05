package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class LoadBalancerMetadataSuccess extends StackEvent {
    public LoadBalancerMetadataSuccess(Long stackId) {
        super(stackId);
    }
}
