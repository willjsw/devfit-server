package com.amcamp.domain.sprint.api;

import com.amcamp.domain.sprint.application.SprintService;
import com.amcamp.domain.sprint.dao.SprintPagingDirection;
import com.amcamp.domain.sprint.dto.request.SprintCreateRequest;
import com.amcamp.domain.sprint.dto.request.SprintUpdateRequest;
import com.amcamp.domain.sprint.dto.response.SprintDetailResponse;
import com.amcamp.domain.sprint.dto.response.SprintIdResponse;
import com.amcamp.domain.sprint.dto.response.SprintInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "5. 스프린트 API", description = "스프린트 관련 API입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/sprints")
public class SprintController {

    private final SprintService sprintService;

    @Operation(summary = "스프린트 생성", description = "스프린트를 생성합니다.")
    @PostMapping("/create")
    public ResponseEntity<SprintInfoResponse> sprintCreate(
            @Valid @RequestBody SprintCreateRequest request) {
        SprintInfoResponse response = sprintService.createSprint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "스프린트 정보 수정", description = "스프린트의 목표, 마감일자를 수정합니다.")
    @PatchMapping("/{sprintId}")
    public SprintInfoResponse sprintUpdate(
            @PathVariable Long sprintId, @Valid @RequestBody SprintUpdateRequest request) {
        return sprintService.updateSprint(sprintId, request);
    }

    @Operation(summary = "스프린트 삭제", description = "스프린트를 삭제합니다.")
    @DeleteMapping("/{sprintId}")
    public ResponseEntity<Void> sprintDelete(@PathVariable Long sprintId) {
        sprintService.deleteSprint(sprintId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Operation(summary = "스프린트 상세 조회", description = "스프린트 기본 정보를 조회합니다.")
    @GetMapping("/{sprintId}")
    public SprintInfoResponse sprintFind(@PathVariable Long sprintId) {
        return sprintService.findSprint(sprintId);
    }

    @Operation(summary = "프로젝트별 스프린트 목록 조회", description = "특정 프로젝트의 스프린트 목록을 조회합니다.")
    @GetMapping("/{projectId}/project")
    public Slice<SprintDetailResponse> sprintFindAll(
            @PathVariable Long projectId,
            @Parameter(description = "기준이 되는 스프린트 ID입니다. 첫 요청 시에는 비워두세요.")
                    @RequestParam(required = false)
                    Long baseSprintId,
            @Parameter(description = "페이징 방향입니다.(NEXT 또는 PREV) 첫 요청 시에는 비워두세요.")
                    @RequestParam(required = false)
                    SprintPagingDirection direction) {
        return sprintService.findAllSprint(projectId, baseSprintId, direction);
    }

    @Operation(summary = "프로젝트별 스프린트 아이디 목록 조회", description = "특정 프로젝트의 스프린트 아이디 목록을 조회합니다.")
    @GetMapping("/{projectId}/list")
    public List<SprintIdResponse> sprintIdAllByMember(@PathVariable Long projectId) {
        return sprintService.findAllSprintId(projectId);
    }

    @Operation(summary = "회원별 프로젝트 내 스프린트 목록 조회", description = "마이페이지에서 특정 프로젝트의 스프린트 목록을 조회합니다.")
    @GetMapping("/{projectId}/me")
    public Slice<SprintDetailResponse> sprintFindAllByMember(
            @PathVariable Long projectId,
            @Parameter(description = "기준이 되는 스프린트 ID입니다. 첫 요청 시에는 비워두세요.")
                    @RequestParam(required = false)
                    Long baseSprintId,
            @Parameter(description = "페이징 방향입니다.(NEXT 또는 PREV) 첫 요청 시에는 비워두세요.")
                    @RequestParam(required = false)
                    SprintPagingDirection direction) {
        return sprintService.findAllSprintByMember(projectId, baseSprintId, direction);
    }
}
