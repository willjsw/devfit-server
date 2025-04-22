package com.amcamp.domain.project.dto.response;

import com.amcamp.domain.project.domain.Project;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record ProjectInfoResponse(
        @Schema(description = "프로젝트 아이디", example = "1") Long projectId,
        @Schema(description = "프로젝트 제목", example = "Devfit") String projectTitle,
        @Schema(description = "프로젝트가 속한 팀 이름", example = "1") String teamName,
        @Schema(description = "프로젝트 설명", example = "LG CNS AM Inspire Camp 사이드 프로젝트")
                String projectDescription,
        @Schema(description = "프로젝트 시작 날짜", example = "2024-01-01") LocalDate startDt,
        @Schema(description = "프로젝트 마감 날짜", example = "2025-01-01") LocalDate dueDt) {

    public static ProjectInfoResponse from(Project project) {
        return new ProjectInfoResponse(
                project.getId(),
                project.getTitle(),
                project.getTeam().getName(),
                project.getDescription(),
                project.getStartDt(),
                project.getDueDt());
    }
}
