package com.taskflow.task.infrastructure.client;

import com.taskflow.task.domain.exception.ServiceUnavailableException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
            log.debug("User {} exists in user-service", userId);
            return true;

        } catch (HttpClientErrorException.NotFound e) {
            log.debug("User {} not found in user-service", userId);
            return false;
        } catch (ResourceAccessException e) {
            log.warn("user-service unavailable for userId={}", userId);
            throw new ServiceUnavailableException("user-service not available");
        } catch (HttpServerErrorException e) {
            log.error("user-service returned {} when checking user {}",
                    e.getStatusCode(), userId);
            throw new ServiceUnavailableException("user-service");
        }
    }

}