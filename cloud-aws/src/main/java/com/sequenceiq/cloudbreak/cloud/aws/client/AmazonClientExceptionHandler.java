package com.sequenceiq.cloudbreak.cloud.aws.client;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import com.amazonaws.SdkClientException;
import com.sequenceiq.cloudbreak.cloud.aws.mapper.SdkClientExceptionMapper;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;

@Aspect
public class AmazonClientExceptionHandler {

    private final AwsCredentialView awsCredentialView;

    private final SdkClientExceptionMapper sdkClientExceptionMapper;

    public AmazonClientExceptionHandler(AwsCredentialView awsCredentialView, SdkClientExceptionMapper sdkClientExceptionMapper) {
        this.awsCredentialView = awsCredentialView;
        this.sdkClientExceptionMapper = sdkClientExceptionMapper;
    }

    @Around("execution(* *(..))")
    public Object mapException(ProceedingJoinPoint proceedingJoinPoint) {
        try {
            return proceedingJoinPoint.proceed();
        } catch (SdkClientException e) {
            throw sdkClientExceptionMapper.map(awsCredentialView, e);
        } catch (Throwable e) {
            throw (RuntimeException) e;
        }
    }

}
