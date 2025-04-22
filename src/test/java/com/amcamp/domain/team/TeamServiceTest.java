package com.amcamp.domain.team;

import static com.amcamp.global.common.constants.RedisConstants.INVITE_CODE_PREFIX;
import static com.amcamp.global.common.constants.RedisConstants.TEAM_ID_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

import com.amcamp.IntegrationTest;
import com.amcamp.domain.member.dao.MemberRepository;
import com.amcamp.domain.member.domain.Member;
import com.amcamp.domain.member.domain.OauthInfo;
import com.amcamp.domain.team.application.TeamService;
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
import com.amcamp.global.security.PrincipalDetails;
import com.amcamp.global.util.MemberUtil;
import com.amcamp.global.util.RedisUtil;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

public class TeamServiceTest extends IntegrationTest {

    @Autowired private TeamRepository teamRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private TeamParticipantRepository teamParticipantRepository;
    @Autowired private TeamService teamService;
    @Autowired private RedisUtil redisUtil;
    @Autowired private MemberUtil memberUtil;

    private void loginAs(Member member) {
        UserDetails userDetails = new PrincipalDetails(member.getId(), member.getRole());
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @BeforeEach
    void setUp() {
        Member member =
                memberRepository.save(
                        Member.createMember(
                                "testNickname",
                                "testProfileImageUrl",
                                OauthInfo.createOauthInfo("testOauthId", "testOauthProvider")));

        UserDetails userDetails = new PrincipalDetails(member.getId(), member.getRole());
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Nested
    class 팀_생성_시 {
        @Test
        void 초대코드를_반환한다() {
            // when
            Member currentMember = memberUtil.getCurrentMember();
            TeamInviteCodeResponse inviteCodeResponse =
                    teamService.createTeam(new TeamCreateRequest("팀 이름", "팀 설명"));

            // then
            assertThat(inviteCodeResponse).isNotNull();
            assertThat(inviteCodeResponse.inviteCode()).isNotNull();
            assertThat(inviteCodeResponse.inviteCode()).hasSize(8);

            // 생성 시 자동으로 설정되는 정보 확인
            TeamCheckResponse teamCheckResponse =
                    teamService.getTeamByCode(
                            new TeamInviteCodeRequest(inviteCodeResponse.inviteCode()));
            Team savedTeam =
                    teamRepository
                            .findById(teamCheckResponse.teamId())
                            .orElseThrow(() -> new CommonException(TeamErrorCode.TEAM_NOT_FOUND));
            TeamParticipant teamParticipant =
                    teamParticipantRepository
                            .findByMemberAndTeam(currentMember, savedTeam)
                            .orElseThrow(
                                    () ->
                                            new CommonException(
                                                    TeamErrorCode.TEAM_PARTICIPANT_REQUIRED));

            assertThat(teamParticipant.getRole()).isEqualTo(TeamParticipantRole.ADMIN);
            assertThat(savedTeam.getEmoji()).isEqualTo("🍇");
        }
    }

    @Nested
    class 팀_아이디로_코드확인_시 {
        @Test
        void 팀이_유효한_경우에는_초대코드를_반환한다() {
            // given
            TeamInviteCodeResponse inviteCodeResponse =
                    teamService.createTeam(new TeamCreateRequest("팀 이름", "팀 설명"));
            Long teamId =
                    teamService
                            .getTeamByCode(
                                    new TeamInviteCodeRequest(inviteCodeResponse.inviteCode()))
                            .teamId();

            // when
            TeamInviteCodeResponse response = teamService.getInviteCode(teamId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.inviteCode()).isNotNull();
            assertThat(response.inviteCode()).hasSize(8);
        }

        @Test
        void 팀이_유효하지않는_경우에는_예외가_발생한다() {
            // given
            Long invalidTeamId = -999L;
            // when & then
            assertThatThrownBy(() -> teamService.getInviteCode(invalidTeamId))
                    .isInstanceOf(CommonException.class)
                    .extracting("errorCode")
                    .isEqualTo(TeamErrorCode.TEAM_NOT_FOUND);
        }

        @Test
        void 팀_참가자가_아닌_경우에는_에러를_반환한다() {
            // given
            TeamInviteCodeResponse inviteCodeResponse =
                    teamService.createTeam(new TeamCreateRequest("팀 이름", "팀 설명"));
            Long teamId =
                    teamService
                            .getTeamByCode(
                                    new TeamInviteCodeRequest(inviteCodeResponse.inviteCode()))
                            .teamId();

            Member nonMember =
                    memberRepository.save(
                            Member.createMember("nonMember", "testProfileImageUrl", null));
            loginAs(nonMember);

            // when & then
            assertThatThrownBy(() -> teamService.getInviteCode(teamId))
                    .isInstanceOf(CommonException.class)
                    .extracting("errorCode")
                    .isEqualTo(TeamErrorCode.TEAM_PARTICIPANT_REQUIRED);
        }
    }

    @Nested
    class 팀_참가_시 {
        @Test
        void 이미_팀에_참가한_경우에는_예외가_발생한다() {
            // given
            TeamInviteCodeResponse inviteCodeResponse =
                    teamService.createTeam(new TeamCreateRequest("팀 이름", "팀 설명"));

            // when & then
            assertThatThrownBy(
                            () ->
                                    teamService.joinTeam(
                                            new TeamInviteCodeRequest(
                                                    inviteCodeResponse.inviteCode())))
                    .isInstanceOf(CommonException.class)
                    .extracting("errorCode")
                    .isEqualTo(TeamErrorCode.MEMBER_ALREADY_JOINED);
        }

        @Test
        @Transactional
        void 새롭게_참여하는_경우에는_팀에_USER로_등록된다() {
            // given
            TeamInviteCodeResponse inviteCodeResponse =
                    teamService.createTeam(new TeamCreateRequest("팀 이름", "팀 설명"));

            String inviteCode = inviteCodeResponse.inviteCode();

            // savedMember 로그인 처리 후 팀 참여
            Member newMember = memberRepository.save(Member.createMember("member", null, null));
            loginAs(newMember);

            // when
            teamService.joinTeam(new TeamInviteCodeRequest(inviteCodeResponse.inviteCode()));

            // then
            TeamCheckResponse teamCheckResponse =
                    teamService.getTeamByCode(new TeamInviteCodeRequest(inviteCode));
            Team savedTeam =
                    teamRepository
                            .findById(teamCheckResponse.teamId())
                            .orElseThrow(() -> new CommonException(TeamErrorCode.TEAM_NOT_FOUND));

            TeamParticipant teamParticipant =
                    teamParticipantRepository
                            .findByMemberAndTeam(newMember, savedTeam)
                            .orElseThrow(
                                    () ->
                                            new CommonException(
                                                    TeamErrorCode.TEAM_PARTICIPANT_REQUIRED));

            assertThat(teamParticipant.getMember()).isEqualTo(newMember);
            assertThat(teamParticipant.getRole()).isEqualTo(TeamParticipantRole.USER);
            assertThat(teamParticipant.getTeam()).isEqualTo(savedTeam);
        }
    }

    @Nested
    class 초대코드로_팀_확인_시 {
        @Test
        void 코드가_유효한_경우에는_팀_정보를_반환한다() {
            // given
            TeamInviteCodeResponse inviteCodeResponse =
                    teamService.createTeam(new TeamCreateRequest("팀 이름", "팀 설명"));

            // when
            TeamCheckResponse teamCheckResponse =
                    teamService.getTeamByCode(
                            new TeamInviteCodeRequest(inviteCodeResponse.inviteCode()));

            // then
            assertThat(teamCheckResponse).isNotNull();
            assertThat(teamCheckResponse.teamId()).isNotNull();

            Team savedTeam =
                    teamRepository
                            .findById(teamCheckResponse.teamId())
                            .orElseThrow(() -> new CommonException(TeamErrorCode.TEAM_NOT_FOUND));

            assertThat(teamCheckResponse.teamName()).isEqualTo(savedTeam.getName());
            assertThat(teamCheckResponse.teamId()).isEqualTo(savedTeam.getId());
        }

        @Test
        void 코드가_유효하지않는_경우에는_예외를_반환한다() {
            // given
            TeamInviteCodeResponse inviteCodeResponse =
                    teamService.createTeam(new TeamCreateRequest("팀 이름", "팀 설명"));

            String invalidInviteCode = "invalidCode";

            // when & then
            assertThatThrownBy(
                            () ->
                                    teamService.getTeamByCode(
                                            new TeamInviteCodeRequest(invalidInviteCode)))
                    .isInstanceOf(CommonException.class)
                    .extracting("errorCode")
                    .isEqualTo(TeamErrorCode.INVALID_INVITE_CODE);
        }
    }

    @Nested
    class 팀_수정_시 {

        @Test
        @Transactional
        void 팀이름과_팀설명을_수정한다() {
            // given
            TeamInviteCodeResponse inviteCodeResponse =
                    teamService.createTeam(new TeamCreateRequest("팀 이름", "팀 설명"));

            TeamCheckResponse teamCheckResponse =
                    teamService.getTeamByCode(
                            new TeamInviteCodeRequest(inviteCodeResponse.inviteCode()));
            Long teamId = teamCheckResponse.teamId();

            // when: all arguments provided
            TeamUpdateRequest teamUpdateRequest = new TeamUpdateRequest("새 팀 이름", "새 팀 설명", "❤️");
            TeamInfoResponse teamInfoResponse = teamService.editTeam(teamId, teamUpdateRequest);

            // then
            assertThat(teamInfoResponse).isNotNull();
            assertThat(teamInfoResponse.teamName()).isEqualTo("새 팀 이름");
            assertThat(teamInfoResponse.teamDescription()).isEqualTo("새 팀 설명");
            assertThat(teamInfoResponse.teamEmoji()).isEqualTo("❤️");

            // when: new team name is missing
            TeamUpdateRequest newTeamUpdateRequest = new TeamUpdateRequest(null, "새 팀 설명-2", "⭐️");
            TeamInfoResponse newTeamInfoResponse =
                    teamService.editTeam(teamId, newTeamUpdateRequest);

            // then
            assertThat(newTeamInfoResponse).isNotNull();
            assertThat(newTeamInfoResponse.teamName()).isEqualTo("새 팀 이름");
            assertThat(newTeamInfoResponse.teamDescription()).isEqualTo("새 팀 설명-2");
            assertThat(newTeamInfoResponse.teamEmoji()).isEqualTo("⭐️");
        }

        @Test
        void 팀이_유효하지않는_경우에는_예외를_반환한다() {
            // given
            TeamInviteCodeResponse inviteCodeResponse =
                    teamService.createTeam(new TeamCreateRequest("팀 이름", "팀 설명"));
            Long invalidTeamId = -999L;

            // when & then
            assertThatThrownBy(
                            () ->
                                    teamService.editTeam(
                                            invalidTeamId,
                                            new TeamUpdateRequest("새 팀 이름", "새 팀 설명", "❤️")))
                    .isInstanceOf(CommonException.class)
                    .extracting("errorCode")
                    .isEqualTo(TeamErrorCode.TEAM_NOT_FOUND);
        }

        @Test
        void 로그인된_회원이_팀_참가자가_아닌_경우에는_예외를_반환한다() {
            // given
            TeamInviteCodeResponse inviteCodeResponse =
                    teamService.createTeam(new TeamCreateRequest("팀 이름", "팀 설명"));

            TeamCheckResponse teamCheckResponse =
                    teamService.getTeamByCode(
                            new TeamInviteCodeRequest(inviteCodeResponse.inviteCode()));
            Long teamId = teamCheckResponse.teamId();

            Member nonMember =
                    memberRepository.save(
                            Member.createMember("nonMember", "testProfileImageUrl", null));
            loginAs(nonMember);

            // when & then
            assertThatThrownBy(
                            () ->
                                    teamService.editTeam(
                                            teamId,
                                            new TeamUpdateRequest("새 팀 이름", "새 팀 설명", "❤️")))
                    .isInstanceOf(CommonException.class)
                    .extracting("errorCode")
                    .isEqualTo(TeamErrorCode.TEAM_PARTICIPANT_REQUIRED); // 사용자 권한이 없을 때
        }

        @Test
        void 로그인된_회원이_팀_관리자가_아닌_경우에는_예외를_반환한다() {
            // given
            TeamInviteCodeResponse inviteCodeResponse =
                    teamService.createTeam(new TeamCreateRequest("팀 이름", "팀 설명"));

            TeamCheckResponse teamCheckResponse =
                    teamService.getTeamByCode(
                            new TeamInviteCodeRequest(inviteCodeResponse.inviteCode()));
            Long teamId = teamCheckResponse.teamId();

            // 일반 사용자 로그인 및 팀 참가
            Member userMember =
                    memberRepository.save(Member.createMember("user", "testProfileImageUrl", null));
            loginAs(userMember);
            teamService.joinTeam(new TeamInviteCodeRequest(inviteCodeResponse.inviteCode()));

            // when & then
            assertThatThrownBy(
                            () ->
                                    teamService.editTeam(
                                            teamId,
                                            new TeamUpdateRequest("새 팀 이름", "새 팀 설명", "❤️")))
                    .isInstanceOf(CommonException.class)
                    .extracting("errorCode")
                    .isEqualTo(TeamErrorCode.UNAUTHORIZED_ACCESS); // 팀 관리자 권한이 없을 경우
        }
    }

    @Nested
    class 팀_삭제_시 {
        @Test
        void 팀을_삭제한다() {
            // given
            TeamInviteCodeResponse inviteCodeResponse =
                    teamService.createTeam(new TeamCreateRequest("팀 이름", "팀 설명"));

            TeamCheckResponse teamCheckResponse =
                    teamService.getTeamByCode(
                            new TeamInviteCodeRequest(inviteCodeResponse.inviteCode()));
            Long teamId = teamCheckResponse.teamId();

            // when
            teamService.deleteTeam(teamId);

            // then
            assertThatThrownBy(
                            () ->
                                    teamService.getTeamByCode(
                                            new TeamInviteCodeRequest(
                                                    inviteCodeResponse.inviteCode())))
                    .isInstanceOf(CommonException.class)
                    .extracting("errorCode")
                    .isEqualTo(TeamErrorCode.INVALID_INVITE_CODE);

            // 1. 팀 삭제 확인
            assertThat(teamRepository.findById(teamId)).isEmpty();
            // 2. 팀 참가자 삭제 확인
            assertThat(teamParticipantRepository.findByTeamId(teamId)).isEmpty();
            // 3. 초대 코드가 Redis에서 삭제되었는지 확인
            Optional<String> inviteCodeInRedisAfterDelete =
                    redisUtil.getData(TEAM_ID_PREFIX.formatted(teamId));
            Optional<String> teamIdInRedisAfterDelete =
                    redisUtil.getData(
                            INVITE_CODE_PREFIX.formatted(inviteCodeResponse.inviteCode()));
            assertThat(inviteCodeInRedisAfterDelete).isEmpty();
            assertThat(teamIdInRedisAfterDelete).isEmpty();
        }

        @Test
        void 팀이_유효하지않는_경우에는_예외를_반환한다() {
            // given
            TeamInviteCodeResponse inviteCodeResponse =
                    teamService.createTeam(new TeamCreateRequest("팀 이름", "팀 설명"));

            Long invalidTeamId = -999L;

            // when & then
            assertThatThrownBy(() -> teamService.deleteTeam(invalidTeamId))
                    .isInstanceOf(CommonException.class)
                    .extracting("errorCode")
                    .isEqualTo(TeamErrorCode.TEAM_NOT_FOUND); // 존재하지 않는 팀 삭제 시 예외
        }

        @Test
        void 로그인된_회원이_팀_참가자가_아닌_경우에는_예외를_반환한다() {
            // given
            TeamInviteCodeResponse inviteCodeResponse =
                    teamService.createTeam(new TeamCreateRequest("팀 이름", "팀 설명"));

            TeamCheckResponse teamCheckResponse =
                    teamService.getTeamByCode(
                            new TeamInviteCodeRequest(inviteCodeResponse.inviteCode()));
            Long teamId = teamCheckResponse.teamId();

            Member nonMember =
                    memberRepository.save(
                            Member.createMember("nonMember", "testProfileImageUrl", null));
            loginAs(nonMember);

            // when & then
            assertThatThrownBy(() -> teamService.deleteTeam(teamId))
                    .isInstanceOf(CommonException.class)
                    .extracting("errorCode")
                    .isEqualTo(TeamErrorCode.TEAM_PARTICIPANT_REQUIRED);
        }

        @Test
        void 로그인된_회원이_팀_관리자가_아닌_경우에는_예외를_반환한다() {
            // given
            TeamInviteCodeResponse inviteCodeResponse =
                    teamService.createTeam(new TeamCreateRequest("팀 이름", "팀 설명"));

            TeamCheckResponse teamCheckResponse =
                    teamService.getTeamByCode(
                            new TeamInviteCodeRequest(inviteCodeResponse.inviteCode()));
            Long teamId = teamCheckResponse.teamId();

            Member userMember =
                    memberRepository.save(Member.createMember("user", "testProfileImageUrl", null));
            loginAs(userMember);
            teamService.joinTeam(new TeamInviteCodeRequest(inviteCodeResponse.inviteCode()));

            // when & then
            assertThatThrownBy(() -> teamService.deleteTeam(teamId))
                    .isInstanceOf(CommonException.class)
                    .extracting("errorCode")
                    .isEqualTo(TeamErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    @Nested
    class 팀_정보_조회_시 {
        @Test
        void 팀이름과_팀설명을_반환한다() {
            // given
            TeamInviteCodeResponse inviteCodeResponse =
                    teamService.createTeam(new TeamCreateRequest("팀 이름", "팀 설명"));

            TeamCheckResponse teamCheckResponse =
                    teamService.getTeamByCode(
                            new TeamInviteCodeRequest(inviteCodeResponse.inviteCode()));
            Long teamId = teamCheckResponse.teamId();

            // when
            TeamInfoResponse teamInfoResponse = teamService.getTeamInfo(teamId);

            // then
            assertThat(teamInfoResponse).isNotNull();
            assertThat(teamInfoResponse.teamName()).isEqualTo("팀 이름");
            assertThat(teamInfoResponse.teamDescription()).isEqualTo("팀 설명");
        }

        @Test
        void 팀이_유효하지않는_경우에는_예외를_반환한다() {
            // given
            TeamInviteCodeResponse inviteCodeResponse =
                    teamService.createTeam(new TeamCreateRequest("팀 이름", "팀 설명"));

            Long invalidTeamId = -999L;

            // when & then
            assertThatThrownBy(() -> teamService.getTeamInfo(invalidTeamId))
                    .isInstanceOf(CommonException.class)
                    .extracting("errorCode")
                    .isEqualTo(TeamErrorCode.TEAM_NOT_FOUND);
        }

        @Test
        void 로그인된_회원이_팀_참가자가_아닌_경우에는_예외를_반환한다() {
            // given
            TeamInviteCodeResponse inviteCodeResponse =
                    teamService.createTeam(new TeamCreateRequest("팀 이름", "팀 설명"));

            TeamCheckResponse teamCheckResponse =
                    teamService.getTeamByCode(
                            new TeamInviteCodeRequest(inviteCodeResponse.inviteCode()));
            Long teamId = teamCheckResponse.teamId();

            Member nonMember =
                    memberRepository.save(
                            Member.createMember("nonMember", "testProfileImageUrl", null));
            loginAs(nonMember);

            // when & then
            assertThatThrownBy(() -> teamService.getTeamInfo(teamId))
                    .isInstanceOf(CommonException.class)
                    .extracting("errorCode")
                    .isEqualTo(TeamErrorCode.TEAM_PARTICIPANT_REQUIRED);
        }
    }

    @Nested
    class 회원이_참여한_팀_목록_조회_시 {
        @Test
        void 회원이_참여한_팀_정보를_반환한다() {
            // given
            List<TeamCreateRequest> requests =
                    List.of(
                            TeamCreateRequest.of("testTeamName1", "testTeamDescription1"),
                            TeamCreateRequest.of("testTeamName2", "testTeamDescription2"),
                            TeamCreateRequest.of("testTeamName3", "testTeamDescription3"));

            for (TeamCreateRequest request : requests) {
                teamService.createTeam(request);
            }

            // when
            Slice<TeamInfoResponse> results = teamService.findAllTeam(null, 3);

            // then
            assertThat(results.getSize()).isEqualTo(3);
            assertThat(results)
                    .extracting("teamId", "teamName", "teamDescription")
                    .containsExactlyInAnyOrder(
                            tuple(3L, "testTeamName3", "testTeamDescription3"),
                            tuple(2L, "testTeamName2", "testTeamDescription2"),
                            tuple(1L, "testTeamName1", "testTeamDescription1"));
        }

        @Test
        void 회원이_참여한_팀이_존재하지_않을_시_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> teamService.findAllTeam(0L, 4))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(TeamErrorCode.TEAM_NOT_EXISTS.getMessage());
        }
    }

    @Test
    void 회원이_참여한_팀의_팀장_정보를_반환한다() {
        // given
        TeamInviteCodeResponse inviteCodeResponse =
                teamService.createTeam(new TeamCreateRequest("팀 이름", "팀 설명"));

        TeamCheckResponse teamCheckResponse =
                teamService.getTeamByCode(
                        new TeamInviteCodeRequest(inviteCodeResponse.inviteCode()));
        Long teamId = teamCheckResponse.teamId();

        // when
        TeamAdminResponse result = teamService.findTeamAdmin(teamId);

        // then
        assertThat(result)
                .extracting("memberId", "nickname", "profileImageUrl")
                .containsExactly(1L, "testNickname", "testProfileImageUrl");

        // 다른 멤버가 팀 참가 후 팀장 정보를 조회했을 때도 동일한지 확인
        Member userMember =
                memberRepository.save(Member.createMember("user", "testProfileImageUrl", null));
        loginAs(userMember);
        teamService.joinTeam(new TeamInviteCodeRequest(inviteCodeResponse.inviteCode()));

        // when
        TeamAdminResponse anotherResult = teamService.findTeamAdmin(teamId);

        // then
        assertThat(anotherResult)
                .extracting("memberId", "nickname", "profileImageUrl")
                .containsExactly(1L, "testNickname", "testProfileImageUrl");
    }
}
