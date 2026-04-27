package com.liveclass.assignment.global.exception;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CustomExceptionCode {

  INVALID_REQUEST(400, "요청 값이 유효하지 않습니다. 입력 값을 확인해주세요."),

  INVALID_TYPE_REQUEST(3000, "요청 본문의 형식 또는 데이터 타입이 올바르지 않습니다."),
  ENROLLMENT_CONFIRM_ONLY_PENDING(3001, "PENDING 상태의 신청만 결제 확정할 수 있습니다."),
  ENROLLMENT_ALREADY_CANCELLED(3002, "이미 취소된 신청입니다."),
  ENROLLMENT_CANCEL_PERIOD_EXPIRED(3003, "취소 가능 기간이 지났습니다."),
  ENROLLMENT_PAYMENT_EXPIRED(3004, "결제 가능 시간이 만료되었습니다."),
  ENROLLMENT_PAYMENT_NOT_EXPIRED(3005, "아직 결제 가능 시간이 만료되지 않았습니다."),
  ENROLLMENT_NOT_CANCELLED(3006, "취소된 신청이 아닙니다."),
  ENROLLMENT_AUTO_CANCEL_ONLY_PENDING(3007, "PENDING 상태의 신청만 자동 취소할 수 있습니다."),
  ENROLLMENT_FORBIDDEN(3008, "해당 수강신청에 대한 권한이 없습니다."),
  CLASS_ROOM_OPEN_ONLY_DRAFT(3009,  "DRAFT 상태의 강의만 모집 시작할 수 있습니다."),
  CLASS_ROOM_CLOSE_ONLY_OPEN(3010, "OPEN 상태의 강의만 모집 마감할 수 있습니다."),
  CLASS_ROOM_UPDATE_ONLY_DRAFT(3011, "DRAFT 상태의 강의만 수정할 수 있습니다."),
  CLASS_ROOM_INVALID_PERIOD(3012, "강의 시작 시각은 종료 시각보다 이전이어야 합니다."),

  CREATOR_NOT_FOUND(3013, "해당 creator가 존재하지 않습니다."),
  CLASS_ROOM_FORBIDDEN(3014, "해당 ClassRoom에 대한 권한이 없습니다."),
  CLASS_ROOM_NOT_FOUND(3015, "해당 ClassRoom이 존재하지 않습니다."),
  INVALID_ROLE(3016, "권한이 존재하지 않습니다."),
  CLASS_ROOM_DRAFT_NOT_VIEWABLE(3017, "Classmate는 'DRAFT' 상태의 ClassRoom을 조회할 수 없습니다."),
  CLASSMATE_NOT_FOUND(3018, "해당 수강생이 존재하지 않습니다."),
  ENROLLMENT_NOT_FOUND(3019, "해당 수강신청 내역이 존재하지 않습니다."),
  CLASS_ROOM_CAPACITY_EXCEEDED(3020, "해당 강의의 수강생 정원이 가득 찼습니다."),
  CLASS_ROOM_NOT_OPEN(3021, "강의가 OPEN 상태가 아닙니다."),
  DUPLICATED_ENROLLMENT(3022, "이미 수강신청 내역이 존재합니다."),
  CLASS_ROOM_ENROLLMENT_COUNT_INVALID(3023, "ClassRoom의 enrollment_count 정합성 오류 발생"),




  INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다.");

  private final int code;
  private final String message;

}
