package com.example.jsppractice.dto;

public record DeliveryImportSkip(Long referenceId, String reason) {

	// JSP 用 getter，理由跟 DeliveryImportResult 上的那兩個一樣
	public Long getReferenceId() {
		return referenceId;
	}

	public String getReason() {
		return reason;
	}
}
