package com.amcamp.domain.task.dto.response;

import com.amcamp.domain.task.domain.SOSStatus;
import com.amcamp.domain.task.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record TaskBasicInfoResponse(
        @Schema(description = "태스크 아이디", example = "1") Long taskId,
        @Schema(description = "태스크 내용", example = "피그마 화면 설계 수정") String description,
        @Schema(description = "태스크 진행 현황", example = "ON_GOING") TaskStatus taskStatus,
        @Schema(description = "태스크 SOS 상태", example = "SOS") SOSStatus sosStatus) {}
