package com.taskflow.task.config;

/**
 * Principal built by {@link HeaderAuthenticationFilter} from the trusted
 * X-User-* headers set by api-gateway (ADR-015). Carries the numeric id so
 * domain security checks (e.g. TaskSecurityService.isOwner) can compare it
 * against entity fields like TaskEntity.assigneeId without a lookup.
 */
public record AuthenticatedUser(Long id, String email) {
}
