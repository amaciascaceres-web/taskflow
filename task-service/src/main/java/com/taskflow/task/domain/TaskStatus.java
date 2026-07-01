package com.taskflow.task.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

import lombok.Getter;

@Getter
public enum TaskStatus {

    TODO("todo"),
    IN_PROGRESS("in-progress"),
    DONE("done");

    @JsonValue
    private final String slug;

    TaskStatus(String slug) {
        this.slug = slug;
    }

    @JsonCreator
    public static TaskStatus fromSlug(String slug) {
        return Arrays.stream(values())
                .filter(s -> s.slug.equalsIgnoreCase(slug))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid status slug: " + slug));
    }
}

