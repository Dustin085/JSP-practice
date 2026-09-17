package com.example.jsppractice.exception;

public class InvalidDeliveryRecordException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InvalidDeliveryRecordException(String message) {
		super(message);
	}
}
