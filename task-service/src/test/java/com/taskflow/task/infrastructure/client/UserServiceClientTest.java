package com.taskflow.task.infrastructure.client;

import com.taskflow.task.domain.exception.ServiceUnavailableException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ExtendWith(MockitoExtension.class)
class UserServiceClientTest {

    @Mock RestTemplate restTemplate;

    UserServiceClient client;

    @BeforeEach
    void setUp() throws Exception {
        client = new UserServiceClient(restTemplate);
        Field urlField = UserServiceClient.class.getDeclaredField("userServiceUrl");
        urlField.setAccessible(true);
        urlField.set(client, "http://user-service:8081");
    }

    @Test
    void userExists_200_returnsTrue() {
        when(restTemplate.getForObject(anyString(), eq(Object.class))).thenReturn(new Object());

        assertThat(client.userExists(1L)).isTrue();
    }

    @Test
    void userExists_404_returnsFalse() {
        when(restTemplate.getForObject(anyString(), eq(Object.class)))
                .thenThrow(HttpClientErrorException.NotFound.class);

        assertThat(client.userExists(1L)).isFalse();
    }

    @Test
    void userExists_connectionRefused_throwsServiceUnavailableException() {
        when(restTemplate.getForObject(anyString(), eq(Object.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> client.userExists(1L))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void userExists_5xx_throwsServiceUnavailableException() {
        when(restTemplate.getForObject(anyString(), eq(Object.class)))
                .thenThrow(new HttpServerErrorException(INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.userExists(1L))
                .isInstanceOf(ServiceUnavailableException.class);
    }
}
