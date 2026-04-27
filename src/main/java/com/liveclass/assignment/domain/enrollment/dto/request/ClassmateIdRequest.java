package com.liveclass.assignment.domain.enrollment.dto.request;

import jakarta.validation.constraints.NotNull;

public record ClassmateIdRequest(

    @NotNull(message = "클래스메이트 ID는 필수입니다.")
    Long classmateId

) {
}