package com.example.jsppractice.exception;

public class EmailAlreadyExistsException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public EmailAlreadyExistsException(String email) {
		super("Email 已經被使用過了：" + email);
	}
}
