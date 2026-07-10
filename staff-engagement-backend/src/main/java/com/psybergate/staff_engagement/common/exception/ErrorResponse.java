package com.psybergate.staff_engagement.common.exception;

import java.util.Map;

public record ErrorResponse(
	String message,
	Map<String, String> fieldErrors
) {}
