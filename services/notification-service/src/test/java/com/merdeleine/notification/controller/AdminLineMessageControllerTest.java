package com.merdeleine.notification.controller;

import com.merdeleine.notification.dto.AdminPushRequest;
import com.merdeleine.notification.entity.LineUser;
import com.merdeleine.notification.repository.LineUserRepository;
import com.merdeleine.notification.service.LineMessagingService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AdminLineMessageControllerTest {

    @Test
    void pushToUserReturnsNotFoundWhenUserMissing() {
        LineMessagingService lineMessagingService = mock(LineMessagingService.class);
        LineUserRepository lineUserRepository = mock(LineUserRepository.class);
        when(lineUserRepository.findByUserId("U-missing")).thenReturn(Optional.empty());

        AdminLineMessageController controller = new AdminLineMessageController(lineMessagingService, lineUserRepository);
        AdminPushRequest request = new AdminPushRequest();
        request.setUserId("U-missing");
        request.setMessage("hello");

        var response = controller.pushToUser(request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        verify(lineMessagingService, never()).pushMessage(anyString(), anyString());
    }

    @Test
    void pushToUserReturnsConflictWhenUserUnfollowed() {
        LineMessagingService lineMessagingService = mock(LineMessagingService.class);
        LineUserRepository lineUserRepository = mock(LineUserRepository.class);

        LineUser lineUser = new LineUser();
        lineUser.setUserId("U-unfollowed");
        lineUser.setFollowed(false);
        when(lineUserRepository.findByUserId("U-unfollowed")).thenReturn(Optional.of(lineUser));

        AdminLineMessageController controller = new AdminLineMessageController(lineMessagingService, lineUserRepository);
        AdminPushRequest request = new AdminPushRequest();
        request.setUserId("U-unfollowed");
        request.setMessage("hello");

        var response = controller.pushToUser(request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        verify(lineMessagingService, never()).pushMessage(anyString(), anyString());
    }

    @Test
    void pushToUserPushesMessageWhenUserFollowed() {
        LineMessagingService lineMessagingService = mock(LineMessagingService.class);
        LineUserRepository lineUserRepository = mock(LineUserRepository.class);

        LineUser lineUser = new LineUser();
        lineUser.setUserId("U-followed");
        lineUser.setFollowed(true);
        when(lineUserRepository.findByUserId("U-followed")).thenReturn(Optional.of(lineUser));

        AdminLineMessageController controller = new AdminLineMessageController(lineMessagingService, lineUserRepository);
        AdminPushRequest request = new AdminPushRequest();
        request.setUserId("U-followed");
        request.setMessage("hello");

        var response = controller.pushToUser(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(lineMessagingService, times(1)).pushMessage("U-followed", "hello");
    }
}

