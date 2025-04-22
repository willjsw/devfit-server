package com.amcamp.domain.contribution.api;

import com.amcamp.domain.contribution.application.ContributionService;
import com.amcamp.domain.contribution.dto.response.BasicContributionInfoResponse;
import com.amcamp.domain.contribution.dto.response.ContributionInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "8. 기여도 API", description = "기여도 관련 API 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/contributions")
public class ContributionController {
    private final ContributionService contributionService;

    @Operation(summary = "개별 회원 기여도 조회", description = "스프린트별 개별 회원의 기여도를 조회합니다.")
    @GetMapping("/{projectId}/me")
    public BasicContributionInfoResponse contributionByMember(
            @PathVariable Long projectId, @RequestParam Long sprintId) {
        return contributionService.getContributionByMember(projectId, sprintId);
    }

    @Operation(summary = "프로젝트 내 회원 기여도 조회", description = "프로젝트 페이지에서 스프린트별 기여도를 조회합니다.")
    @GetMapping("/{sprintId}")
    public List<ContributionInfoResponse> contributionBySprint(@PathVariable Long sprintId) {
        return contributionService.getContributionBySprint(sprintId);
    }
}
