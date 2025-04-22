package com.amcamp.domain.sprint.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record SprintUpdateRequest(
        @Schema(description = "중간 목표", example = "MVP 개발") String goal,
        @JsonFormat(
                        shape = JsonFormat.Shape.STRING,
                        pattern = "yyyy-MM-dd",
                        timezone = "Asia/Seoul")
                @Schema(description = "스프린트 마감 날짜", defaultValue = "2026-03-01")
                LocalDate dueDt) {}
