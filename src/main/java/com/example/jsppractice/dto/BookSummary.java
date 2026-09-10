package com.example.jsppractice.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.example.jsppractice.model.Category;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookSummary {
	private Long id;

	private String title;

	private String isbn;

	private Integer publishedYear;

	private String authorName;

	private List<Category> categories;

	public String getCategoriesString() {
		if (categories == null) {
			return "";
		}
		return categories.stream().map((category) -> category.getName()).collect(Collectors.joining(", "));
	};
}
