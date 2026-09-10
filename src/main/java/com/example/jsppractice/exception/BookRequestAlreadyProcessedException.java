package com.example.jsppractice.exception;

public class BookRequestAlreadyProcessedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public BookRequestAlreadyProcessedException(String message) {
		super(message);
	}
}
