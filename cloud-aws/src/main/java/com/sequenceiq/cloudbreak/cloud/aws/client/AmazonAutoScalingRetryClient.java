package com.sequenceiq.cloudbreak.cloud.aws.client;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesResult;
import com.amazonaws.services.autoscaling.model.DetachInstancesRequest;
import com.amazonaws.services.autoscaling.model.DetachInstancesResult;
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest;
import com.amazonaws.services.autoscaling.model.ResumeProcessesResult;
import com.amazonaws.services.autoscaling.model.SuspendProcessesRequest;
import com.amazonaws.services.autoscaling.model.SuspendProcessesResult;
import com.amazonaws.services.autoscaling.model.TerminateInstanceInAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.TerminateInstanceInAutoScalingGroupResult;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupResult;
import com.sequenceiq.cloudbreak.cloud.aws.mapper.SdkClientExceptionMapper;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.service.Retry;

public class AmazonAutoScalingRetryClient extends AmazonClient {

    private final AmazonAutoScalingClient client;

    private final Retry retry;

    public AmazonAutoScalingRetryClient(AmazonAutoScalingClient client, AwsCredentialView awsCredentialView, SdkClientExceptionMapper sdkClientExceptionMapper,
            Retry retry) {
        super(awsCredentialView, sdkClientExceptionMapper);
        this.client = client;
        this.retry = retry;
    }

    public SuspendProcessesResult suspendProcesses(SuspendProcessesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapSdkClientException(() -> client.suspendProcesses(request)));
    }

    public ResumeProcessesResult resumeProcesses(ResumeProcessesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapSdkClientException(() -> client.resumeProcesses(request)));
    }

    public DescribeAutoScalingGroupsResult describeAutoScalingGroups(DescribeAutoScalingGroupsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapSdkClientException(() -> client.describeAutoScalingGroups(request)));
    }

    public UpdateAutoScalingGroupResult updateAutoScalingGroup(UpdateAutoScalingGroupRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapSdkClientException(() -> client.updateAutoScalingGroup(request)));
    }

    public DetachInstancesResult detachInstances(DetachInstancesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapSdkClientException(() -> client.detachInstances(request)));
    }

    public DescribeScalingActivitiesResult describeScalingActivities(DescribeScalingActivitiesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapSdkClientException(() -> client.describeScalingActivities(request)));
    }

    public TerminateInstanceInAutoScalingGroupResult terminateInstance(TerminateInstanceInAutoScalingGroupRequest terminateInstanceInAutoScalingGroupRequest) {
        return retry.testWith2SecDelayMax15Times(() -> mapSdkClientException(() ->
                client.terminateInstanceInAutoScalingGroup(terminateInstanceInAutoScalingGroupRequest)));
    }
}
