package com.amcamp.domain.feedback.api;

import com.amcamp.domain.feedback.application.FeedbackService;
import com.amcamp.domain.feedback.dto.request.FeedbackSendRequest;
import com.amcamp.domain.feedback.dto.request.OriginalFeedbackRequest;
import com.amcamp.domain.feedback.dto.response.FeedbackInfoResponse;
import com.amcamp.domain.feedback.dto.response.FeedbackRefineResponse;
import com.amcamp.domain.project.dto.response.ProjectParticipantFeedbackInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "7. 피드백 API", description = "피드백 관련 API입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "스프린트별 동료평가 여부 확인", description = "스프린트별/팀원별 동료평가 여부를 확인합니다. ")
    @GetMapping("/{sprintId}/participants")
    public Slice<ProjectParticipantFeedbackInfoResponse> feedbackStatusFind(
            @PathVariable Long sprintId,
            @Parameter(description = "프로젝트 아이디") @RequestParam Long projectId,
            @Parameter(description = "이전 페이지의 마지막 프로젝트 참가자 ID (첫 페이지는 비워두세요)")
                    @RequestParam(required = false)
                    Long lastProjectParticipantId,
            @Parameter(description = "페이지당 프로젝트 참여자 수", example = "5") @RequestParam(value = "size")
                    int pageSize) {

        return feedbackService.findFeedbackStatusBySprint(
                projectId, sprintId, lastProjectParticipantId, pageSize);
    }

    @Operation(
            summary = "OpenAI 기반 피드백 메시지 개선",
            description = "사용자가 입력한 피드백을 AI가 분석하여 부드럽고 명확하게 개선합니다.")
    @PostMapping("/refinement")
    public FeedbackRefineResponse feedbackRefine(
            @Valid @RequestBody OriginalFeedbackRequest request) {
        return feedbackService.refineFeedback(request);
    }

    @Operation(summary = "개선된 피드백 메시지 전송", description = "사용자가 AI를 통해 개선한 피드백을 특정 팀원에게 전송합니다.")
    @PostMapping("/sent")
    public ResponseEntity<Void> feedbackSend(@Valid @RequestBody FeedbackSendRequest request) {
        feedbackService.sendFeedback(request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "피드백 메시지 조회", description = "특정 프로젝트 참여자가 스프린트에서 받은 피드백을 조회합니다.")
    @GetMapping("/{projectId}")
    public Slice<FeedbackInfoResponse> participantFindSprintFeedbacks(
            @PathVariable Long projectId,
            @Parameter(description = "피드백을 조회할 스프린트 ID") @RequestParam Long sprintId,
            @Parameter(description = "이전 페이지의 마지막 피드백 ID (첫 페이지는 비워두세요)")
                    @RequestParam(required = false)
                    Long lastFeedbackId,
            @Parameter(description = "페이지당 피드백 수", example = "1") @RequestParam(value = "size")
                    int pageSize) {
        return feedbackService.findSprintFeedbacksByParticipant(
                projectId, sprintId, lastFeedbackId, pageSize);
    }
}
