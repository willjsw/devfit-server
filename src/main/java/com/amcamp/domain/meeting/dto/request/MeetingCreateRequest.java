package com.amcamp.domain.meeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record MeetingCreateRequest(
        @Schema(description = "스프린트 아이디", example = "1") @NotNull Long sprintId,
        @Schema(description = "미팅 타이틀", example = "중간 점검 회의")
                @Size(max = 15, message = "미팅 타이틀은 최대 15자까지 입력 가능합니다.")
                @NotBlank
                String title,
        @Schema(description = "미팅 시작 날짜/시간", example = "2026-03-01T15:17:00") @NotNull
                LocalDateTime meetingStart,
        @Schema(description = "미팅 종료 날짜/시간", example = "2026-03-01T15:18:00") @NotNull
                LocalDateTime meetingEnd) {}
