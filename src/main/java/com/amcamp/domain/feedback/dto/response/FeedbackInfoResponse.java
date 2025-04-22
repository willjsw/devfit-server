package com.amcamp.domain.feedback.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FeedbackInfoResponse(
        @Schema(description = "피드백 ID", example = "1") Long feedbackId,
        @Schema(description = "받은 피드백 메시지", example = "이번 스프린트에서 아주 잘해주셨습니다.") String message,
        @Schema(description = "피드백을 받은 날짜", example = "2026-01-01") LocalDate receivedDt) {
    public FeedbackInfoResponse(Long feedbackId, String message, LocalDateTime createdDt) {
        this(feedbackId, message, createdDt.toLocalDate());
    }
}
