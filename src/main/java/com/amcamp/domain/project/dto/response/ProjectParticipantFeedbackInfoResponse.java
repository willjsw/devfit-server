package com.amcamp.domain.project.dto.response;

import com.amcamp.domain.project.domain.ProjectParticipantRole;
import com.amcamp.domain.project.domain.ProjectParticipantStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProjectParticipantFeedbackInfoResponse(
        @Schema(description = "프로젝트 참여자 아이디", example = "1") Long projectParticipantId,
        @Schema(description = "프로젝트 참여자 이름", example = "정선우") String nickname,
        @Schema(description = "프로젝트 참여자 이미지 url", example = "PreSigned URL") String profileImageUrl,
        @Schema(description = "프로젝트 참여자 권한", example = "PROJECT_ADMIN") ProjectParticipantRole role,
        @Schema(description = "프로젝트 참여자 참여 상태", example = "ACTIVE") ProjectParticipantStatus status,
        @Schema(description = "동료평가 완료 여부", example = "COMPLETED") String feedbackStatus) {}
