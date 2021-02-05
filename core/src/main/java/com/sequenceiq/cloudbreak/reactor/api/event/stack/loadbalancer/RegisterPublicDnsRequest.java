package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RegisterPublicDnsRequest extends StackEvent {

    public RegisterPublicDnsRequest(Long stackId) {
        super(stackId);
    }
}

