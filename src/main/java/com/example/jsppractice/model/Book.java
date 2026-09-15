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

	// 樂觀鎖版本號，不是使用者填的欄位（表單裡是 hidden input，跟著編輯當下讀到的值原封不動送回來）。
	// 新書一律從 0 開始，@Builder.Default 是必要的：沒有它 Book.builder().build() 在沒指定 version
	// 時會直接是 null，這個欄位初始值只有走 new Book()（no-args 建構子）才會生效。
	@Builder.Default
	private Integer version = 0;
}
