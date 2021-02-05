package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CreateLoadBalancerEntityRequest extends StackEvent {

    public CreateLoadBalancerEntityRequest(Long stackId) {
        super(stackId);
    }
}
