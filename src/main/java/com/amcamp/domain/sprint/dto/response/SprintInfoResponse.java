package com.amcamp.domain.sprint.dto.response;

import com.amcamp.domain.sprint.domain.Sprint;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record SprintInfoResponse(
        @Schema(description = "스프린트 ID", example = "1") Long id,
        @Schema(description = "스프린트 제목", example = "1차 스프린트") String title,
        @Schema(description = "중간 목표", example = "MVP 개발") String goal,
        @Schema(description = "스프린트 시작 날짜", defaultValue = "2026-02-01") LocalDate startDt,
        @Schema(description = "스프린트 마감 날짜", defaultValue = "2026-03-01") LocalDate dueDt,
        @Schema(description = "스프린트 진척도", defaultValue = "0") Integer progress) {
    public static SprintInfoResponse from(Sprint sprint) {
        return new SprintInfoResponse(
                sprint.getId(),
                sprint.getTitle(),
                sprint.getGoal(),
                sprint.getStartDt(),
                sprint.getDueDt(),
                sprint.getProgress().intValue());
    }
}
