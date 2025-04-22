package com.amcamp.domain.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamCreateRequest(
        @NotBlank(message = "팀 이름은 필수 사항입니다.")
                @Size(max = 15, message = "팀 이름은 최대 15자까지 입력 가능합니다.")
                @Schema(description = "팀 이름", example = "Side Effect")
                String teamName,
        @Size(max = 100, message = "팀 설명은 최대 100자까지 입력 가능합니다.")
                @Schema(description = "팀 설명", example = "Lg cns am camp 1기 개발 스터디")
                String teamDescription) {
    public static TeamCreateRequest of(String teamName, String teamDescription) {
        return new TeamCreateRequest(teamName, teamDescription);
    }
}
