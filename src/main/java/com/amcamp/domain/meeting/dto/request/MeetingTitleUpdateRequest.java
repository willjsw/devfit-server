package com.amcamp.domain.meeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MeetingTitleUpdateRequest(
        @Schema(description = "수정된 미팅 타이틀", example = "중간 점검 회의")
                @Size(max = 15, message = "미팅 타이틀은 최대 15자까지 입력 가능합니다.")
                @NotBlank
                String title) {}
