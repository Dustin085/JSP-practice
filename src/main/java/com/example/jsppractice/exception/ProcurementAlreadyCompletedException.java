package com.example.jsppractice.exception;

public class ProcurementAlreadyCompletedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ProcurementAlreadyCompletedException(String message) {
		super(message);
	}
}
