package com.amcamp.domain.task.api;

import com.amcamp.domain.task.application.TaskService;
import com.amcamp.domain.task.dto.request.TaskBasicInfoUpdateRequest;
import com.amcamp.domain.task.dto.request.TaskCreateRequest;
import com.amcamp.domain.task.dto.response.TaskBasicInfoResponse;
import com.amcamp.domain.task.dto.response.TaskInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "6. 태스크 API", description = "태스크  관련 API입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/tasks")
public class TaskController {
    private final TaskService taskService;

    @Operation(summary = "태스크 생성", description = "태스크를 생성합니다.")
    @PostMapping("/create")
    public TaskInfoResponse taskCreate(@Valid @RequestBody TaskCreateRequest request) {
        return taskService.createTask(request);
    }

    @Operation(summary = "태스크 기본 정보 수정", description = "태스크 기본 정보를 수정합니다.")
    @PatchMapping("/{taskId}/basic-info")
    public TaskInfoResponse taskUpdateBasic(
            @PathVariable Long taskId, @Valid @RequestBody TaskBasicInfoUpdateRequest request) {
        return taskService.updateTaskBasicInfo(taskId, request);
    }

    @Operation(summary = "태스크 완료", description = "태스크 완료에 따른 일정 및 진행 상태를 수정합니다.")
    @PatchMapping("/{taskId}")
    public TaskInfoResponse taskUpdateToDo(@PathVariable Long taskId) {
        return taskService.updateTaskStatus(taskId);
    }

    @Operation(summary = "태스크 SOS", description = "태스크 sos 상태를 수정합니다.")
    @PatchMapping("/{taskId}/sos")
    public TaskInfoResponse taskUpdateSOS(@PathVariable Long taskId) {
        return taskService.updateTaskSOS(taskId);
    }

    @Operation(summary = "태스크 담당자 할당", description = "태스크 담당 상태를 수정합니다.")
    @PostMapping("/{taskId}")
    public TaskInfoResponse taskAssign(@PathVariable Long taskId) {
        return taskService.assignTask(taskId);
    }

    @Operation(summary = "태스크 삭제", description = "태스크를 삭제합니다.")
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> taskDelete(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Operation(summary = "태스크 목록 조회", description = "태스크를 스프린트 아이디값에 따라 불러 옵니다.")
    @GetMapping("/{sprintId}/sprint")
    public Slice<TaskInfoResponse> taskList(
            @PathVariable Long sprintId,
            @Parameter(description = "이전 페이지의 마지막 태스크 ID (첫 페이지는 비워두세요)")
                    @RequestParam(required = false)
                    Long lastTaskId,
            @RequestParam(value = "size", defaultValue = "3") int size) {
        return taskService.getTasksBySprint(sprintId, lastTaskId, size);
    }

    @Deprecated
    @Operation(
            summary = "마이 페이지 내 스프린트별 태스크 조회",
            description = "멤버에 할당된 태스크를 스프린트 아이디값에 따라 불러 옵니다.")
    @GetMapping("/{sprintId}/me")
    public Slice<TaskBasicInfoResponse> taskListByMember(
            @PathVariable Long sprintId,
            @Parameter(description = "이전 페이지의 마지막 태스크 ID (첫 페이지는 비워두세요)")
                    @RequestParam(required = false)
                    Long lastTaskId,
            @RequestParam(value = "size", defaultValue = "3") int size) {
        return taskService.getTasksByMember(sprintId, lastTaskId, size);
    }
}
