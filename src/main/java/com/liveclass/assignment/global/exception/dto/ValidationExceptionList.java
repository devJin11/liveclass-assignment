package com.liveclass.assignment.global.exception.dto;

import java.util.Map;

public record ValidationExceptionList(int totalErrorCount, int code, Map<String, String> errors) {}
