package com.liveclass.assignment.domain.classroom.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreatorIdRequest(
    @NotNull(message = "크리에이터 ID는 필수입니다.")
    Long creatorId
) {
}
