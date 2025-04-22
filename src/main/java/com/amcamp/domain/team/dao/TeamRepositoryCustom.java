package com.amcamp.domain.team.dao;

import com.amcamp.domain.team.dto.response.TeamInfoResponse;
import java.util.List;
import org.springframework.data.domain.Slice;

public interface TeamRepositoryCustom {
    Slice<TeamInfoResponse> findAllTeamByMemberId(Long memberId, Long lastTeamId, int pageSize);

    List<TeamInfoResponse> findAllTeamByMemberId(Long memberId);
}
