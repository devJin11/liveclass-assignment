package com.liveclass.assignment.global.exception;

import lombok.Getter;

@Getter
public class ForbiddenException extends RuntimeException {

  private final int code;
  private final String message;

  public ForbiddenException(CustomExceptionCode customExceptionCode) {
    this.code = customExceptionCode.getCode();
    this.message = customExceptionCode.getMessage();
  }

}