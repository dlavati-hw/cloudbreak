package com.sequenceiq.cloudbreak.cloud.aws.client;

import java.util.function.Supplier;

import com.amazonaws.SdkClientException;
import com.sequenceiq.cloudbreak.cloud.aws.mapper.SdkClientExceptionMapper;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;

public abstract class AmazonClient {

    private final AwsCredentialView awsCredentialView;

    private final SdkClientExceptionMapper sdkClientExceptionMapper;

    public AmazonClient(AwsCredentialView awsCredentialView, SdkClientExceptionMapper sdkClientExceptionMapper) {
        this.awsCredentialView = awsCredentialView;
        this.sdkClientExceptionMapper = sdkClientExceptionMapper;
    }

    protected <T> T mapSdkClientException(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (SdkClientException e) {
            throw sdkClientExceptionMapper.map(awsCredentialView, e);
        }
    }
}
