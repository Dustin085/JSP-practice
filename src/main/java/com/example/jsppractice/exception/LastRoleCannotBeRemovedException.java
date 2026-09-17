package com.example.jsppractice.exception;

public class LastRoleCannotBeRemovedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public LastRoleCannotBeRemovedException(String message) {
		super(message);
	}
}
