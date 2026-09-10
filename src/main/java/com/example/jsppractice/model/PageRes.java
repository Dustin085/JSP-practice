package com.example.jsppractice.model;

import java.util.List;

public record PageRes<T>(List<T> content, int pageNumber, int pageSize, long totalElements) {
	public int numberOfElements() {
		return content.size();
	}

	public long totalPages() {
		return (totalElements + pageSize - 1) / pageSize;
	}
}
