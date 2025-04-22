package com.amcamp.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NicknameUpdateRequest(
        @NotBlank(message = "닉네임은 비워둘 수 없습니다.")
                @Size(max = 10, message = "닉네임은 최대 10자까지 입력 가능합니다.")
                @Schema(description = "닉네임", example = "최현태")
                String nickname) {}
