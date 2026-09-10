package com.example.jsppractice.exception;

public class SelfReviewNotAllowedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SelfReviewNotAllowedException(String message) {
		super(message);
	}
}
