package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CreateCloudLoadBalancersSuccess extends StackEvent {
    public CreateCloudLoadBalancersSuccess(Long stackId) {
        super(stackId);
    }
}
