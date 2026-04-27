package com.liveclass.assignment.global.exception;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {

  private final int code;
  private final String message;

  public BadRequestException(CustomExceptionCode customExceptionCode) {
    this.code = customExceptionCode.getCode();
    this.message = customExceptionCode.getMessage();
  }

}
