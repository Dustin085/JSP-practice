package com.example.jsppractice.model;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

	private Long id;

	@NotBlank(message = "書名為必填")
	private String title;

	@Pattern(regexp = "^$|^(97[89])?\\d{9}[\\dXx]$", message = "ISBN 格式不正確")
	private String isbn;

	private Long authorId;

	@NotNull(message = "出版年為必填")
	@Min(value = 1450, message = "出版年不能早於 1450 年")
	@Max(value = 2100, message = "出版年不能晚於 2100 年")
	private Integer publishedYear;
}
