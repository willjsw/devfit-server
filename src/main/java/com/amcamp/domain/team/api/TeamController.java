package com.amcamp.domain.team.api;

import com.amcamp.domain.team.application.TeamService;
import com.amcamp.domain.team.dto.request.TeamCreateRequest;
import com.amcamp.domain.team.dto.request.TeamInviteCodeRequest;
import com.amcamp.domain.team.dto.request.TeamUpdateRequest;
import com.amcamp.domain.team.dto.response.TeamAdminResponse;
import com.amcamp.domain.team.dto.response.TeamCheckResponse;
import com.amcamp.domain.team.dto.response.TeamInfoResponse;
import com.amcamp.domain.team.dto.response.TeamInviteCodeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "3. 팀 API", description = "팀 관련 API입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/teams")
public class TeamController {
    private final TeamService teamService;

    @Operation(summary = "팀 생성", description = "팀을 생성합니다.")
    @PostMapping("/create")
    public TeamInviteCodeResponse teamCreate(
            @Valid @RequestBody TeamCreateRequest teamCreateRequest) {
        return teamService.createTeam(teamCreateRequest);
    }

    @Operation(summary = "코드 확인", description = "팀 가입을 위한 초대 코드를 확인합니다.")
    @GetMapping("/{teamId}/invite-code")
    public TeamInviteCodeResponse teamInvite(@PathVariable Long teamId) {
        return teamService.getInviteCode(teamId);
    }

    @Operation(summary = "팀 참가 전 팀 확인", description = "초대 코드를 입력하여 참여하려고 하는 팀 정보를 확인합니다.")
    @PostMapping("/check")
    public TeamCheckResponse teamCheck(
            @Valid @RequestBody TeamInviteCodeRequest teamInviteCodeRequest) {
        return teamService.getTeamByCode(teamInviteCodeRequest);
    }

    @Operation(summary = "팀 참가", description = "팀 정보를 확인 후 팀에 참가합니다.")
    @PostMapping("/join")
    public ResponseEntity<Void> teamJoin(
            @Valid @RequestBody TeamInviteCodeRequest teamInviteCodeRequest) {
        teamService.joinTeam(teamInviteCodeRequest);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "팀 수정", description = "팀 이름과 설명, 이모지 등 팀 기본정보를 수정합니다.")
    @PatchMapping("/{teamId}")
    public TeamInfoResponse teamEdit(
            @PathVariable Long teamId, @Valid @RequestBody TeamUpdateRequest teamUpdateRequest) {
        return teamService.editTeam(teamId, teamUpdateRequest);
    }

    @Operation(summary = "팀 삭제", description = "팀을 삭제합니다.")
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> teamDelete(@PathVariable Long teamId) {
        teamService.deleteTeam(teamId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "팀 정보", description = "팀 이름과 설명 등 기본 정보를 반환합니다.")
    @GetMapping("/{teamId}")
    public TeamInfoResponse teamInfo(@PathVariable Long teamId) {
        return teamService.getTeamInfo(teamId);
    }

    @Operation(summary = "팀장 조회", description = "팀 페이지에서 팀장을 조회합니다.")
    @GetMapping("/{teamId}/admin")
    public TeamAdminResponse teamFindAdmin(@PathVariable Long teamId) {
        return teamService.findTeamAdmin(teamId);
    }

    @Deprecated
    @Operation(summary = "팀 목록 조회", description = "회원이 참여한 팀 목록을 조회합니다.")
    @GetMapping("/list")
    public Slice<TeamInfoResponse> teamFindAll(
            @Parameter(description = "이전 페이지의 마지막 팀 ID (첫 페이지는 비워두세요)")
                    @RequestParam(required = false)
                    Long lastTeamId,
            @Parameter(description = "페이지당 팀 수", example = "1") @RequestParam(value = "size")
                    int pageSize) {
        return teamService.findAllTeam(lastTeamId, pageSize);
    }

    @Operation(summary = "팀 목록 조회 V2", description = "회원이 참여한 팀 목록을 조회합니다.")
    @GetMapping("/list/all")
    public List<TeamInfoResponse> teamFindAll() {
        return teamService.findAllTeam();
    }
}
