package com.example.jsppractice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/*
	 * 找不到對應 Controller 的 404，是靠 web.xml 的 <error-page> 處理，
	 * 不是這裡：handler 為 null 時 ExceptionHandlerExceptionResolver 不會生效，
	 * @ExceptionHandler 天生接不到 NoHandlerFoundException。
	 */

	/*
	 * 非預期例外 (Exception)
	 */
	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public String handleGeneralException(Exception e, Model model) {
		log.error("未預期的例外", e);
		model.addAttribute("message", "伺服器發生未預期的錯誤");
		return "error/500";
	}
}
