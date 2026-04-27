package com.liveclass.assignment.domain.classroom.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ClassRoomRequest(

    @NotNull(message = "크리에이터 ID는 필수입니다.")
    Long creatorId,

    @NotBlank(message = "강의 제목은 필수입니다.")
    @Size(max = 100, message = "강의 제목은 100자를 초과할 수 없습니다.")
    String title,

    @NotBlank(message = "강의 설명은 필수입니다.")
    @Size(max = 4000, message = "강의 설명은 4000자를 초과할 수 없습니다.")
    String description,

    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.") // 무료 강의 존재
    Long price,

    @NotNull(message = "정원은 필수입니다.")
    @Min(value = 10, message = "최소 정원은 10명 이상이어야 합니다.") // 요구사항 없어서 내 맘대로
    Integer capacity,

    @NotNull(message = "수강 시작 일시는 필수입니다.")
    LocalDateTime startAt,

    @NotNull(message = "수강 종료 일시는 필수입니다.")
    LocalDateTime endAt
) {
}
