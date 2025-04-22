package com.amcamp.domain.meeting.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MeetingStatus {
    OPEN("MEETING_OPEN"),
    CLOSE("MEETING_CLOSE");

    private final String status;
}
