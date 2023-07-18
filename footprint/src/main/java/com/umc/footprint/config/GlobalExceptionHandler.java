package com.umc.footprint.config;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(Exception.class)
	public BaseResponse<BaseResponseStatus> handleException(Exception exception) {
		log.error("{}", exception);
		return new BaseResponse<>(BaseResponseStatus.INTERNAL_SERVER_ERROR);
	}
}
