package com.amcamp.domain.team.application;

import static com.amcamp.global.common.constants.RedisConstants.*;

import com.amcamp.domain.member.domain.Member;
import com.amcamp.domain.team.dao.TeamParticipantRepository;
import com.amcamp.domain.team.dao.TeamRepository;
import com.amcamp.domain.team.domain.Team;
import com.amcamp.domain.team.domain.TeamParticipant;
import com.amcamp.domain.team.domain.TeamParticipantRole;
import com.amcamp.domain.team.dto.request.TeamCreateRequest;
import com.amcamp.domain.team.dto.request.TeamInviteCodeRequest;
import com.amcamp.domain.team.dto.request.TeamUpdateRequest;
import com.amcamp.domain.team.dto.response.TeamAdminResponse;
import com.amcamp.domain.team.dto.response.TeamCheckResponse;
import com.amcamp.domain.team.dto.response.TeamInfoResponse;
import com.amcamp.domain.team.dto.response.TeamInviteCodeResponse;
import com.amcamp.global.exception.CommonException;
import com.amcamp.global.exception.errorcode.TeamErrorCode;
import com.amcamp.global.util.MemberUtil;
import com.amcamp.global.util.RandomUtil;
import com.amcamp.global.util.RedisUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {
    private final MemberUtil memberUtil;
    private final TeamRepository teamRepository;
    private final TeamParticipantRepository teamParticipantRepository;
    private final RedisUtil redisUtil;
    private final RandomUtil randomUtil;

    public TeamInviteCodeResponse createTeam(TeamCreateRequest teamCreateRequest) {
        Member member = memberUtil.getCurrentMember();
        Team team =
                Team.createTeam(
                        normalizeTeamName(teamCreateRequest.teamName()),
                        teamCreateRequest.teamDescription());
        teamRepository.save(team);

        TeamParticipant teamParticipant =
                TeamParticipant.createParticipant(member, team, TeamParticipantRole.ADMIN);
        teamParticipantRepository.save(teamParticipant);

        String code = randomUtil.generateCode(team.getId());

        return new TeamInviteCodeResponse(code);
    }

    public TeamInviteCodeResponse getInviteCode(Long teamId) {
        Member member = memberUtil.getCurrentMember();
        Team team = validateTeam(teamId);
        validateAdminParticipant(member, team);

        String code = randomUtil.generateCode(team.getId());

        return new TeamInviteCodeResponse(code);
    }

    public TeamCheckResponse getTeamByCode(TeamInviteCodeRequest teamInviteCodeRequest) {
        Team team = searchTeamByCode(teamInviteCodeRequest.inviteCode());
        return new TeamCheckResponse(team.getId(), team.getName());
    }

    public void joinTeam(TeamInviteCodeRequest teamInviteCodeRequest) {
        Member member = memberUtil.getCurrentMember();
        Team team = searchTeamByCode(teamInviteCodeRequest.inviteCode());
        validateTeamJoin(member, team);

        TeamParticipant teamParticipant =
                TeamParticipant.createParticipant(member, team, TeamParticipantRole.USER);
        teamParticipantRepository.save(teamParticipant);
    }

    public TeamInfoResponse editTeam(Long teamId, TeamUpdateRequest teamUpdateRequest) {
        Member member = memberUtil.getCurrentMember();
        Team team = validateTeam(teamId);
        validateAdminParticipant(member, team);

        TeamUpdateRequest normalizedTeamUpdateRequest =
                new TeamUpdateRequest(
                        normalizeTeamName(teamUpdateRequest.teamName()),
                        teamUpdateRequest.teamDescription(),
                        teamUpdateRequest.teamEmoji());

        team.updateTeam(normalizedTeamUpdateRequest);

        return TeamInfoResponse.from(team);
    }

    public void deleteTeam(Long teamId) {
        Member member = memberUtil.getCurrentMember();
        Team team = validateTeam(teamId);
        validateAdminParticipant(member, team);

        teamParticipantRepository.deleteByTeam(team);

        redisUtil
                .getData(TEAM_ID_PREFIX.formatted(teamId))
                .ifPresent(
                        inviteCode -> {
                            redisUtil.deleteData(INVITE_CODE_PREFIX.formatted(inviteCode));
                            redisUtil.deleteData(TEAM_ID_PREFIX.formatted(teamId));
                        });

        teamRepository.delete(team);
    }

    @Transactional(readOnly = true)
    public TeamInfoResponse getTeamInfo(Long teamId) {
        Member member = memberUtil.getCurrentMember();
        Team team = validateTeam(teamId);
        validateParticipant(member, team);

        return TeamInfoResponse.from(team);
    }

    private Team searchTeamByCode(String inviteCode) {
        Long teamId =
                redisUtil
                        .getData(INVITE_CODE_PREFIX.formatted(inviteCode))
                        .map(Long::valueOf)
                        .orElseThrow(() -> new CommonException(TeamErrorCode.INVALID_INVITE_CODE));

        return teamRepository
                .findById(teamId)
                .orElseThrow(() -> new CommonException(TeamErrorCode.TEAM_NOT_FOUND));
    }

    private String normalizeTeamName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return name;
        }
        return name.trim().replaceAll("[^0-9a-zA-Z가-힣 ]", "_");
    }

    private Team validateTeam(Long teamId) {
        Team team =
                teamRepository
                        .findById(teamId)
                        .orElseThrow(() -> new CommonException(TeamErrorCode.TEAM_NOT_FOUND));
        return team;
    }

    private void validateTeamJoin(Member member, Team team) {
        if (teamParticipantRepository.findByMemberAndTeam(member, team).isPresent()) {
            throw new CommonException(TeamErrorCode.MEMBER_ALREADY_JOINED);
        }
    }

    private void validateParticipant(Member member, Team team) {
        if (!teamParticipantRepository.findByMemberAndTeam(member, team).isPresent()) {
            throw new CommonException(TeamErrorCode.TEAM_PARTICIPANT_REQUIRED);
        }
    }

    private void validateAdminParticipant(Member member, Team team) {
        TeamParticipant teamParticipant =
                teamParticipantRepository
                        .findByMemberAndTeam(member, team)
                        .orElseThrow(
                                () -> new CommonException(TeamErrorCode.TEAM_PARTICIPANT_REQUIRED));

        if (teamParticipant.getRole() != TeamParticipantRole.ADMIN) {
            throw new CommonException(TeamErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    @Transactional(readOnly = true)
    public Slice<TeamInfoResponse> findAllTeam(Long lastTeamId, int pageSize) {
        Member currentMember = memberUtil.getCurrentMember();
        return teamRepository.findAllTeamByMemberId(currentMember.getId(), lastTeamId, pageSize);
    }

    @Transactional(readOnly = true)
    public List<TeamInfoResponse> findAllTeam() {
        Member currentMember = memberUtil.getCurrentMember();
        return teamRepository.findAllTeamByMemberId(currentMember.getId());
    }

    @Transactional(readOnly = true)
    public TeamAdminResponse findTeamAdmin(Long teamId) {
        Member member = memberUtil.getCurrentMember();
        Team team = validateTeam(teamId);
        validateParticipant(member, team);
        return TeamAdminResponse.from(
                teamParticipantRepository.findAdmin(teamId, TeamParticipantRole.ADMIN));
    }
}
