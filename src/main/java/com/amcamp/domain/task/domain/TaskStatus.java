package com.amcamp.domain.task.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TaskStatus {
    NOT_STARTED("STATUS_NOT_STARTED"),
    ON_GOING("STATUS_ON_GOING"),
    COMPLETED("STATUS_COMPLETED");

    private final String status;
}
