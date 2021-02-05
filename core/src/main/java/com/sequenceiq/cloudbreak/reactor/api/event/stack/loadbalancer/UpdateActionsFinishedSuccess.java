package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpdateActionsFinishedSuccess extends StackEvent {
    public UpdateActionsFinishedSuccess(Long stackId) {
        super(stackId);
    }
}