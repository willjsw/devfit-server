package com.amcamp.domain.contribution.dto.response;

import com.amcamp.domain.contribution.domain.Contribution;
import io.swagger.v3.oas.annotations.media.Schema;

public record BasicContributionInfoResponse(
        @Schema(description = "기여도 아이디", example = "1") Long contributionId,
        @Schema(description = "스프린트 아이디", example = "1") Long sprintId,
        @Schema(description = "프로젝트 참여자 아이디", example = "1") Long projectParticipantId,
        @Schema(description = "기여도 점수", example = "68.1") int score) {

    public static BasicContributionInfoResponse of(Contribution contribution, Double score) {
        return new BasicContributionInfoResponse(
                contribution.getId(),
                contribution.getSprint().getId(),
                contribution.getParticipant().getId(),
                score.intValue());
    }
}
