package com.example.jsppractice.exception;

public class SftpOperationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public SftpOperationException(String message, Throwable cause) {
		super(message, cause);
	}
}
