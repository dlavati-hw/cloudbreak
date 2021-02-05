package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.handler;

import static java.util.Objects.requireNonNull;

import java.util.Set;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reactor.bus.Event;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterPublicDnsFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterPublicDnsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterPublicDnsSuccess;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

@Component
public class RegisterPublicDnsHandler extends ExceptionCatcherEventHandler<RegisterPublicDnsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterPublicDnsHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RegisterPublicDnsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RegisterPublicDnsRequest> event) {
        return new RegisterPublicDnsFailure(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        RegisterPublicDnsRequest request = event.getData();
        Stack stack = stackService.getById(request.getResourceId());
        requireNonNull(stack);
        try {
            LOGGER.debug("Fetching instance group and instance metadata for stack.");
            Set<InstanceGroup> instanceGroups = instanceGroupService.findByStackId(stack.getId());
            instanceGroups.forEach(ig -> ig.setInstanceMetaData(instanceMetaDataService.findByInstanceGroup(ig)));
            stack.setInstanceGroups(instanceGroups);
            LOGGER.debug("Registering load balancer public DNS entry");
            boolean success = clusterPublicEndpointManagementService.provisionLoadBalancer(stack);
            if (!success) {
                throw new CloudbreakException("Public DNS registration resulted in failed state. Please consult DNS registration logs.");
            }
            LOGGER.debug("Load balancer public DNS registration was successful");
            return new RegisterPublicDnsSuccess(stack);
        } catch (Exception e) {
            return new RegisterPublicDnsFailure(request.getResourceId(), e);
        }
    }
}
