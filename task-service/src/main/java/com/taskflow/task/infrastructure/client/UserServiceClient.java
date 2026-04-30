package com.taskflow.task.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.user-service.url:http://localhost:8081}")
    private String userServiceUrl;

    public boolean userExists(Long userId) {
        try {
            restTemplate.getForObject(
                    userServiceUrl + "/api/users/" + userId,
                    Object.class
            );
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }
}