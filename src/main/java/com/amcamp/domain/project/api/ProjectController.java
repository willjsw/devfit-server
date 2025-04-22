package com.amcamp.domain.project.api;

import com.amcamp.domain.project.application.ProjectService;
import com.amcamp.domain.project.dto.request.*;
import com.amcamp.domain.project.dto.response.ProjectInfoResponse;
import com.amcamp.domain.project.dto.response.ProjectListInfoResponse;
import com.amcamp.domain.project.dto.response.ProjectParticipantInfoResponse;
import com.amcamp.domain.project.dto.response.ProjectRegisterDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "4. 프로젝트 API", description = "프로젝트 관련 API 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "프로젝트 생성", description = "새로운 프로젝트를 생성합니다.")
    @PostMapping("/create")
    public ResponseEntity<Void> projectCreate(
            @Valid @RequestBody ProjectCreateRequest projectCreateRequest) {
        projectService.createProject(projectCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(
            summary = "전체 프로젝트 목록 조회",
            description = "팀 ID를 통해 사용자가 참여 중인 프로젝트와 참여 중이지 않은 프로젝트를 나누어 조회합니다.")
    @GetMapping("/{teamId}/list")
    public Slice<ProjectListInfoResponse> projectListInfo(
            @PathVariable Long teamId,
            @RequestParam(required = false) Long lastProjectId,
            @RequestParam(defaultValue = "10") int pageSize) {
        return projectService.getProjectListInfo(teamId, lastProjectId, pageSize);
    }

    @Operation(summary = "프로젝트 조회", description = "프로젝트 ID를 통해 프로젝트 정보를 조회합니다.")
    @GetMapping("/{projectId}")
    public ProjectInfoResponse projectInfo(@PathVariable Long projectId) {
        return projectService.getProjectInfo(projectId);
    }

    @Operation(summary = "프로젝트 정보 수정", description = "프로젝트 제목, 설명, 마감일자를 수정합니다.")
    @PatchMapping("/{projectId}")
    public ProjectInfoResponse projectUpdate(
            @PathVariable Long projectId, @Valid @RequestBody ProjectUpdateRequest request) {
        return projectService.updateProject(projectId, request);
    }

    @Operation(summary = "프로젝트 삭제", description = "프로젝트를 삭제합니다.")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> projectDelete(@PathVariable Long projectId) {
        projectService.deleteProject(projectId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "프로젝트 가입 신청", description = "프로젝트 가입 신청 요청을 보냅니다.")
    @PostMapping("/{projectId}/registration")
    public ResponseEntity<Void> projectRegister(@PathVariable Long projectId) {
        projectService.requestToProjectRegistration(projectId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "프로젝트 가입 신청 목록 조회", description = "현재 프로젝트에 신청된 가입 요청 목록을 조회합니다.")
    @GetMapping("/{projectId}/registration/list")
    public Slice<ProjectRegisterDetailResponse> projectRegistrationListGet(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long lastRegistrationId,
            @RequestParam(defaultValue = "10") int pageSize) {
        return projectService.getProjectRegistrationList(projectId, lastRegistrationId, pageSize);
    }

    @Operation(summary = "프로젝트 가입 신청 승인", description = "프로젝트 가입 신청을 승인합니다.")
    @PutMapping("/{projectId}/registration/approve")
    public ResponseEntity<Void> projectRegistrationApprove(
            @PathVariable Long projectId, @RequestParam Long projectRegisterId) {
        projectService.approveProjectRegistration(projectId, projectRegisterId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "프로젝트 가입 신청 거부", description = "프로젝트 가입 신청을 거부합니다.")
    @PutMapping("/{projectId}/registration/reject")
    public ResponseEntity<Void> projectRegistrationReject(
            @PathVariable Long projectId, @RequestParam Long projectRegisterId) {
        projectService.rejectProjectRegistration(projectId, projectRegisterId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "프로젝트 가입 신청 취소", description = "프로젝트 가입 신청을 취소합니다.")
    @DeleteMapping("/{projectId}/registration/cancel")
    public ResponseEntity<Void> projectRegistrationDelete(@PathVariable Long projectId) {
        projectService.deleteProjectRegistration(projectId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "프로젝트 내 개인 참가자 정보 조회", description = "로그인한 사용자의 해당 프로젝트 참가자 정보를 조회합니다.")
    @GetMapping("/{projectId}/me")
    public ProjectParticipantInfoResponse projectParticipantGet(@PathVariable Long projectId) {
        return projectService.getProjectParticipant(projectId);
    }

    @Operation(summary = "프로젝트 참가자 목록 조회", description = "현재 프로젝트에 참여하고 있는 참가자 전체 목록을 조회합니다. ")
    @GetMapping("/{projectId}/participants")
    public Slice<ProjectParticipantInfoResponse> projectParticipantListGet(
            @PathVariable Long projectId,
            @Parameter(description = "이전 페이지의 마지막 프로젝트 참가자 ID (첫 페이지는 비워두세요)")
                    @RequestParam(required = false)
                    Long lastProjectParticipantId,
            @Parameter(description = "페이지당 프로젝트 참여자 수", example = "1") @RequestParam(value = "size")
                    int pageSize) {
        return projectService.getProjectParticipantList(
                projectId, lastProjectParticipantId, pageSize);
    }

    @Operation(summary = "프로젝트 나가기", description = "프로젝트에 참여중인 프로젝트 참여자 정보를 삭제합니다.")
    @DeleteMapping("/{projectId}/leave")
    public ResponseEntity<Void> projectParticipantDelete(@PathVariable Long projectId) {
        projectService.deleteProjectParticipant(projectId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "admin 권한 양도", description = "프로젝트 Admin 권한을 양도합니다.")
    @PostMapping("/{projectId}/admin/change")
    public ResponseEntity<Void> projectAdminChange(
            @PathVariable Long projectId, @Valid @RequestBody ProjectAdminChangeRequest request) {
        projectService.changeProjectAdmin(projectId, request.newAdminId());
        return ResponseEntity.ok().build();
    }
}
