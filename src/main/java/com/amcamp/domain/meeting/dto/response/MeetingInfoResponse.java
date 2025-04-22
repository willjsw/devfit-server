package com.amcamp.domain.meeting.dto.response;

import com.amcamp.domain.meeting.domain.Meeting;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record MeetingInfoResponse(
        @Schema(description = "미팅 아이디", example = "1") @NotNull Long meetingId,
        @Schema(description = "미팅 타이틀", example = "미팅 타이틀") @NotNull String meetingTitle,
        @Schema(description = "미팅 시작날짜, 시간", example = "회의 일시") @NotNull LocalDateTime meetingStart,
        @Schema(description = "미팅 종료날짜, 시간", example = "회의 일시") @NotNull LocalDateTime meetingEnd) {
    public static MeetingInfoResponse from(Meeting meeting) {
        return new MeetingInfoResponse(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getMeetingStart(),
                meeting.getMeetingEnd());
    }
}
