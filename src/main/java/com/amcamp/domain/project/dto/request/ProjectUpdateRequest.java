package com.amcamp.domain.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ProjectUpdateRequest(
        @Schema(description = "수정된 프로젝트 제목", example = "new project title")
                @Size(max = 15, message = "프로젝트 제목은 최대 15자까지 입력 가능합니다.")
                String title,
        @Schema(description = "수정된 프로젝트 설명", example = "new project description")
                @Size(max = 100, message = "프로젝트 설명은 최대 100자까지 입력 가능합니다.")
                String description,
        @Schema(description = "수정된 프로젝트 마감일자", example = "2026-04-01") LocalDate dueDt) {}
