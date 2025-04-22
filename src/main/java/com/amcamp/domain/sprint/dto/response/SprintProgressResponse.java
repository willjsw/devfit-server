package com.amcamp.domain.sprint.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SprintProgressResponse(
        @Schema(description = "스프린트 아이디", example = "1L") Long sprintId,
        @Schema(description = "스프린트 진척도", example = "50.0") Double progress) {
    public static SprintProgressResponse from(Long sprintId, Double progress) {
        return new SprintProgressResponse(sprintId, progress);
    }
}
