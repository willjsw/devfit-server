package com.amcamp.domain.meeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import java.time.LocalDateTime;

public record MeetingDtUpdateRequest(
        @Schema(description = "수정된 미팅 시작날짜, 시간", example = "회의 일시") @Nullable
                LocalDateTime meetingStart,
        @Schema(description = "수정된 미팅 종료날짜, 시간", example = "회의 일시") @Nullable
                LocalDateTime meetingEnd) {}
