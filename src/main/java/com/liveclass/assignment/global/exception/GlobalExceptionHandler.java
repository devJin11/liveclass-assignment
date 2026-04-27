package com.liveclass.assignment.global.exception;

import com.liveclass.assignment.global.exception.dto.ExceptionResponse;
import com.liveclass.assignment.global.exception.dto.ValidationExceptionList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.liveclass.assignment.global.exception.CustomExceptionCode.*;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  /**
   * @Valid 검증 실패 예외 처리기
   * */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException e,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request)
  {
    log.warn("Validation failed: {}", e.getMessage());

    Map<String, String> errorMap = e.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            FieldError::getDefaultMessage,
            (existing, replacement) -> existing,
            LinkedHashMap::new // 발생 순서 유지
        ));

    //validation 예외 응답 DTO 구성
    ValidationExceptionList response = new ValidationExceptionList(
        errorMap.size(),
        INVALID_REQUEST.getCode(),
        errorMap
    );

    return ResponseEntity.badRequest().body(response);
  }

  /**
   *  JSON 파싱/타입 오류 처리 :
   *  1. JSON 문법 오류
   *  2. 타입 불일지
   *  즉, 요청 body를 java 객체로 역직렬화 실패 시 호출
   */
  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
                                                                HttpHeaders headers,
                                                                HttpStatusCode status,
                                                                WebRequest request) {
    return ResponseEntity.badRequest()
        .body(new ExceptionResponse(
            INVALID_TYPE_REQUEST.getCode(),
            INVALID_TYPE_REQUEST.getMessage()));
  }

  /**
   *  커스텀 예외 타입(단순함을 위해 대부분 요청 or 비즈니스 예외를 이 타입으로 변환)
   * */
  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ExceptionResponse> handleBadRequestException(final BadRequestException e){

    return ResponseEntity.badRequest()
        .body(new ExceptionResponse(e.getCode(), e.getMessage()));
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ExceptionResponse> handleNotFoundException(NotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ExceptionResponse(e.getCode(), e.getMessage()));
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ExceptionResponse> handleForbiddenException(final ForbiddenException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ExceptionResponse(e.getCode(), e.getMessage()));
  }

  /**
   * 잡지 못한 모든 예외를 최종적으로 잡아주는 fallback handler
   * */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ExceptionResponse> handleException(Exception e){
    log.error(e.getMessage(), e);

    return ResponseEntity.internalServerError() // 500 에러
        .body(new ExceptionResponse(INTERNAL_SERVER_ERROR.getCode(), INTERNAL_SERVER_ERROR.getMessage()));

  }


}
