package com.amcamp.domain.sprint.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SprintCreateRequest(
        @NotNull(message = "프로젝트 ID는 비워둘 수 없습니다.") @Schema(description = "프로젝트 ID", example = "1")
                Long projectId,
        @NotBlank(message = "스프린트 중간 목표는 비워둘 수 없습니다.")
                @Size(max = 100, message = "스프린트 중간 목표는 최대 100자까지 입력 가능합니다.")
                @Schema(description = "중간 목표", example = "MVP 개발")
                String goal,
        @NotNull(message = "스프린트 마감 날짜는 비워둘 수 없습니다.")
                @JsonFormat(
                        shape = JsonFormat.Shape.STRING,
                        pattern = "yyyy-MM-dd",
                        timezone = "Asia/Seoul")
                @Schema(description = "스프린트 마감 날짜", defaultValue = "2026-03-01")
                LocalDate dueDt) {
    public static SprintCreateRequest of(Long projectId, String goal, LocalDate dueDt) {
        return new SprintCreateRequest(projectId, goal, dueDt);
    }
}
