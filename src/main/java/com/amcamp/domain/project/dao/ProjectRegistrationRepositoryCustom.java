package com.amcamp.domain.project.dao;

import com.amcamp.domain.project.dto.response.ProjectRegisterDetailResponse;
import org.springframework.data.domain.Slice;

public interface ProjectRegistrationRepositoryCustom {
    Slice<ProjectRegisterDetailResponse> findAllByProjectIdWithPagination(
            Long projectId, Long lastRegistrationId, int pageSize);
}
