package com.taskflow.task.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskPriorityTest {

    @ParameterizedTest
    @CsvSource({
        "low,    LOW",
        "medium, MEDIUM",
        "high,   HIGH"
    })
    void fromSlug_validSlug_returnsCorrectPriority(String slug, TaskPriority expected) {
        assertThat(TaskPriority.fromSlug(slug.trim())).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"LOW", "Medium", "HIGH"})
    void fromSlug_caseInsensitive_resolves(String slug) {
        assertThat(TaskPriority.fromSlug(slug)).isNotNull();
    }

    @Test
    void fromSlug_unknownSlug_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> TaskPriority.fromSlug("critical"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("critical");
    }

    @Test
    void slug_values_matchApiContract() {
        assertThat(TaskPriority.LOW.getSlug()).isEqualTo("low");
        assertThat(TaskPriority.MEDIUM.getSlug()).isEqualTo("medium");
        assertThat(TaskPriority.HIGH.getSlug()).isEqualTo("high");
    }
}
