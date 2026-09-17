package com.example.jsppractice.exception;

public class BaselineRoleCannotBeRevokedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public BaselineRoleCannotBeRevokedException(String message) {
		super(message);
	}
}
