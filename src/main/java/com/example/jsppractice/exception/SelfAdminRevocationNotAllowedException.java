package com.example.jsppractice.exception;

public class SelfAdminRevocationNotAllowedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SelfAdminRevocationNotAllowedException(String message) {
		super(message);
	}
}
