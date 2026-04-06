package com.taskflow.task.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

import lombok.Getter;

@Getter
public enum TaskPriority {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private final String slug;

    TaskPriority(String slug) {
        this.slug = slug;
    }

    @JsonCreator
    public static TaskPriority fromSlug(String slug) {
        return Arrays.stream(values())
                .filter(p -> p.slug.equalsIgnoreCase(slug))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid priority slug: " + slug));
    }
}
