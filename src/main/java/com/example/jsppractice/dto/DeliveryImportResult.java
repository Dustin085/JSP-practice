package com.example.jsppractice.dto;

import java.util.List;

public record DeliveryImportResult(int appliedCount, List<DeliveryImportSkip> skipped) {

	// JSP 用：這個專案的 EL 實作（javax.el 3.0.1-b12，比 record 還早的版本）不認得 record
	// 自動產生的 appliedCount()/skipped()，${result.appliedCount} 這種寫法只認 JavaBean
	// 的 getAppliedCount()，所以另外補這兩個 getter，Java 端邏輯照樣用 appliedCount()/skipped()
	public int getAppliedCount() {
		return appliedCount;
	}

	public List<DeliveryImportSkip> getSkipped() {
		return skipped;
	}
}
