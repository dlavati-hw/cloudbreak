package com.sequenceiq.distrox.v1.distrox.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.service.list.ListAuthorizationService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.distrox.v1.distrox.StackOperations;

@ExtendWith(MockitoExtension.class)
class DistroXV1ControllerTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String CRN = "crn";

    private static final String NAME = "name";

    @Mock
    private StackOperations stackOperations;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private ListAuthorizationService listAuthorizationService;

    @InjectMocks
    private DistroXV1Controller distroXV1Controller;

    @Test
    void testListUsesListAuthorizationService() {
        StackViewV4Responses expected = new StackViewV4Responses();
        when(listAuthorizationService.getResultAs()).thenReturn(expected);

        StackViewV4Responses actual = distroXV1Controller.list(NAME, CRN);

        assertEquals(expected, actual);
        verify(listAuthorizationService).getResultAs();
    }
}