package com.liveclass.assignment.global.exception;

import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException {

  private final int code;
  private final String message;

  public NotFoundException(CustomExceptionCode customExceptionCode) {
    this.code = customExceptionCode.getCode();
    this.message = customExceptionCode.getMessage();
  }
}
