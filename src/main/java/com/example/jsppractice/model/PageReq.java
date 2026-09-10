package com.example.jsppractice.model;

public record PageReq(int pageNumber, int pageSize) {
	public PageReq {
		if (pageNumber < 0) {
			throw new IllegalArgumentException("頁碼不能小於 0");
		}
		if (pageSize <= 0) {
			throw new IllegalArgumentException("每頁筆數必須大於 0");
		}
	}

	public long offset() {
		return (long) pageNumber * pageSize;
	}
}
