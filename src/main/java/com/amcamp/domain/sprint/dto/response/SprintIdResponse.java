package com.amcamp.domain.sprint.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SprintIdResponse(
        @Schema(description = "스프린트 ID", example = "1") Long sprintId,
        @Schema(description = "스프린트 제목", example = "1차 스프린트") String title) {}
