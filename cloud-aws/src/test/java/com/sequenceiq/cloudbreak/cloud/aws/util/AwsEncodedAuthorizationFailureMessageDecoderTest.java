package com.sequenceiq.cloudbreak.cloud.aws.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.AWSSecurityTokenServiceException;
import com.amazonaws.services.securitytoken.model.DecodeAuthorizationMessageRequest;
import com.amazonaws.services.securitytoken.model.DecodeAuthorizationMessageResult;
import com.github.jknack.handlebars.internal.Files;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;

@ExtendWith(MockitoExtension.class)
class AwsEncodedAuthorizationFailureMessageDecoderTest {

    private static String DECODED_MESSAGE;

    private static final String ENCODED_AUTHORIZATION_FAILURE_MESSAGE =
            "API: ec2:CreateSecurityGroup You are not authorized to perform this operation. Encoded authorization failure message: encoded-message";

    @Mock
    private AwsClient awsClient;

    @Mock
    private AWSSecurityTokenService awsSecurityTokenService;

    @Mock
    private AwsCredentialView awsCredentialView;

    @InjectMocks
    private AwsEncodedAuthorizationFailureMessageDecoder underTest;

    @Captor
    ArgumentCaptor<DecodeAuthorizationMessageRequest> requestCaptor;

    @BeforeAll
    static void init() throws IOException {
        DECODED_MESSAGE = Files.read(new File("src/test/resources/json/aws-decoded-authorization-error.json"));
    }

    @BeforeEach
    void setUp() {
        lenient().when(awsClient.createAwsSecurityTokenService(any()))
                .thenReturn(awsSecurityTokenService);
        lenient().when(awsSecurityTokenService.decodeAuthorizationMessage(any()))
                .thenReturn(new DecodeAuthorizationMessageResult().withDecodedMessage(DECODED_MESSAGE));
    }

    @Test
    void shouldReturnUnmodifiedMessageWhenMessageIsNotEncoded() {
        String message = "Resource never entered the desired state as it failed.";

        String result = underTest.decodeAuthorizationFailureMessageIfNeeded(awsCredentialView, message);

        assertThat(result).isEqualTo(message);
        verifyNoInteractions(awsClient);
        verifyNoInteractions(awsSecurityTokenService);
    }

    @Test
    void shouldDecodeEncodedMessage() {
        String result = underTest.decodeAuthorizationFailureMessageIfNeeded(awsCredentialView, ENCODED_AUTHORIZATION_FAILURE_MESSAGE);

        assertThat(result).isEqualTo("You are not authorized to perform action ec2:CreateSecurityGroup " +
                "on resource arn:aws:ec2:eu-central-1:123456789101:vpc/vpc-id");
        verify(awsClient).createAwsSecurityTokenService(awsCredentialView);
        verify(awsSecurityTokenService).decodeAuthorizationMessage(requestCaptor.capture());
        DecodeAuthorizationMessageRequest request = requestCaptor.getValue();
        assertThat(request.getEncodedMessage()).isEqualTo("encoded-message");
    }

    @Test
    void shouldReturnMessageWithWarningWhenStsAccessIsDenied() {
        AWSSecurityTokenServiceException exception = new AWSSecurityTokenServiceException("AccessDenied");
        exception.setErrorCode("AccessDenied");
        when(awsSecurityTokenService.decodeAuthorizationMessage(any()))
                .thenThrow(exception);

        String result = underTest.decodeAuthorizationFailureMessageIfNeeded(awsCredentialView, ENCODED_AUTHORIZATION_FAILURE_MESSAGE);
        assertThat(result).isEqualTo("API: ec2:CreateSecurityGroup You are not authorized to perform this operation. " +
                "(Please add sts:DecodeAuthorizationMessage right to your IAM policy to get more details.)");
        verify(awsClient).createAwsSecurityTokenService(awsCredentialView);
        verify(awsSecurityTokenService).decodeAuthorizationMessage(any());
    }

    @Test
    void shouldReturnUnmodifiedMessageWhenStsThrowsOtherException() {
        AWSSecurityTokenServiceException exception = new AWSSecurityTokenServiceException("SomethingWentWrong");
        when(awsSecurityTokenService.decodeAuthorizationMessage(any()))
                .thenThrow(exception);

        String result = underTest.decodeAuthorizationFailureMessageIfNeeded(awsCredentialView, ENCODED_AUTHORIZATION_FAILURE_MESSAGE);
        assertThat(result).isEqualTo(ENCODED_AUTHORIZATION_FAILURE_MESSAGE);
        verify(awsClient).createAwsSecurityTokenService(awsCredentialView);
        verify(awsSecurityTokenService).decodeAuthorizationMessage(any());
    }

    @Test
    void shouldReturnUnmodifiedMessageWhenNonStsExceptionIsThrown() {
        Exception exception = new RuntimeException("SomethingWentWrong");
        when(awsSecurityTokenService.decodeAuthorizationMessage(any()))
                .thenThrow(exception);

        String result = underTest.decodeAuthorizationFailureMessageIfNeeded(awsCredentialView, ENCODED_AUTHORIZATION_FAILURE_MESSAGE);
        assertThat(result).isEqualTo(ENCODED_AUTHORIZATION_FAILURE_MESSAGE);
        verify(awsClient).createAwsSecurityTokenService(awsCredentialView);
        verify(awsSecurityTokenService).decodeAuthorizationMessage(any());
    }

}
