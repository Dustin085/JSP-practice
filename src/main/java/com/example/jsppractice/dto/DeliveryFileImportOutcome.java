package com.example.jsppractice.dto;

public record DeliveryFileImportOutcome(String fileName, DeliveryImportResult result, String errorMessage) {

	public boolean isSucceeded() {
		return errorMessage == null;
	}

	// JSP 用 getter，理由跟 DeliveryImportResult 上的那兩個一樣
	public String getFileName() {
		return fileName;
	}

	public DeliveryImportResult getResult() {
		return result;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
