package com.amcamp.domain.project.dto.response;

import com.amcamp.domain.project.domain.ProjectRegistration;
import com.amcamp.domain.project.domain.ProjectRegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProjectRegisterDetailResponse(
        @Schema(description = "프로젝트 아이디", example = "1") Long projectId,
        @Schema(description = "프로젝트 가입 요청 아이디", example = "1") Long registrationId,
        @Schema(description = "프로젝트 가입 요청자 아이디", example = "1") Long requesterId,
        @Schema(description = "프로젝트 가입 요청자 회원 닉네임", example = "최현태") String requesterNickname,
        @Schema(description = "프로젝트 가입 요청자 회원 프로필 이미지", example = "presigend image ")
                String requesterImageUrl,
        @Schema(description = "프로젝트 가입 요청 진행 상태", example = "REQUEST_PENDING")
                ProjectRegistrationStatus projectRegistrationStatus) {

    public static ProjectRegisterDetailResponse from(ProjectRegistration projectRegistration) {
        return new ProjectRegisterDetailResponse(
                projectRegistration.getProject().getId(),
                projectRegistration.getId(),
                projectRegistration.getRequester().getId(),
                projectRegistration.getRequester().getMember().getNickname(),
                projectRegistration.getRequester().getMember().getProfileImageUrl(),
                projectRegistration.getRequestStatus());
    }
}
