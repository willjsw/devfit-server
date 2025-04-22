package com.amcamp.domain.project.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProjectParticipantStatus {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    ;

    private final String status;
}
