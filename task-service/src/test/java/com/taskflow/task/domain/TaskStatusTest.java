package com.taskflow.task.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskStatusTest {

    @ParameterizedTest
    @CsvSource({
        "todo,        TODO",
        "in-progress, IN_PROGRESS",
        "done,        DONE"
    })
    void fromSlug_validSlug_returnsCorrectStatus(String slug, TaskStatus expected) {
        assertThat(TaskStatus.fromSlug(slug.trim())).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"TODO", "In-Progress", "DONE"})
    void fromSlug_caseInsensitive_resolves(String slug) {
        assertThat(TaskStatus.fromSlug(slug)).isNotNull();
    }

    @Test
    void fromSlug_unknownSlug_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> TaskStatus.fromSlug("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void slug_values_matchApiContract() {
        assertThat(TaskStatus.TODO.getSlug()).isEqualTo("todo");
        assertThat(TaskStatus.IN_PROGRESS.getSlug()).isEqualTo("in-progress");
        assertThat(TaskStatus.DONE.getSlug()).isEqualTo("done");
    }
}
