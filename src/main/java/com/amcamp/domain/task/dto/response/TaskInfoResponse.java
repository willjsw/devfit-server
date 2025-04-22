package com.amcamp.domain.task.dto.response;

import com.amcamp.domain.member.domain.Member;
import com.amcamp.domain.task.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record TaskInfoResponse(
        @Schema(description = "태스크 아이디", example = "1") Long taskId,
        @Schema(description = "태스크 내용", example = "피그마 화면 설계 수정") String description,
        @Schema(description = "태스크 난이도", example = "MID") TaskDifficulty taskDifficulty,
        @Schema(description = "태스크 완료일자", example = "2024-01-02") LocalDate dueDt,
        @Schema(description = "태스크 진행 현황", example = "ON_GOING") TaskStatus taskStatus,
        @Schema(description = "태스크 담당 상태", example = "ASSIGNED") AssignedStatus assignedStatus,
        @Schema(description = "태스크 SOS 상태", example = "SOS") SOSStatus sosStatus,
        @Schema(description = "태스크 담당자 아이디", example = "1") Long projectParticipantId,
        @Schema(description = "태스크 담당자 닉네임", example = "최현태") String nickname,
        @Schema(description = "태스크 담당자 프로필 url", example = "Presigned URL")
                String profileImageUrl) {

    public static TaskInfoResponse from(Task task, Member member) {
        return new TaskInfoResponse(
                task.getId(),
                task.getDescription(),
                task.getTaskDifficulty(),
                task.getDueDt(),
                task.getTaskStatus(),
                task.getAssignedStatus(),
                task.getSosStatus(),
                member.getId(),
                member.getNickname(),
                member.getProfileImageUrl());
    }

    public static TaskInfoResponse from(Task task) {
        return new TaskInfoResponse(
                task.getId(),
                task.getDescription(),
                task.getTaskDifficulty(),
                task.getDueDt(),
                task.getTaskStatus(),
                task.getAssignedStatus(),
                task.getSosStatus(),
                null,
                null,
                null);
    }
}
