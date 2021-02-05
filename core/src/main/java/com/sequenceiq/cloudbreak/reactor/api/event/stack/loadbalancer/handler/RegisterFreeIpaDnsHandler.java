package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.handler;

import static java.util.Objects.requireNonNull;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reactor.bus.Event;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterFreeIpaDnsFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterFreeIpaDnsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterFreeIpaDnsSuccess;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

@Component
public class RegisterFreeIpaDnsHandler extends ExceptionCatcherEventHandler<RegisterFreeIpaDnsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterFreeIpaDnsHandler.class);

    @Inject
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RegisterFreeIpaDnsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RegisterFreeIpaDnsRequest> event) {
        return new RegisterFreeIpaDnsFailure(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        RegisterFreeIpaDnsRequest request = event.getData();
        Stack stack = request.getStack();
        requireNonNull(stack);
        try {
            LOGGER.debug("Registering load balancer DNS entry with FreeIPA");
            clusterPublicEndpointManagementService.registerLoadBalancerWithFreeIPA(stack);
            LOGGER.debug("Load balancer FreeIPA DNS registration was successful");
            return new RegisterFreeIpaDnsSuccess(stack);
        } catch (Exception e) {
            return new RegisterFreeIpaDnsFailure(request.getResourceId(), e);
        }
    }
}
