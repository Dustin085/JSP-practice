package com.example.jsppractice.model;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookRequestItem {

	private Long id;

	private Long bookRequestId;

	@NotBlank(message = "書名為必填")
	private String title;

	@Pattern(regexp = "^$|^(97[89])?\\d{9}[\\dXx]$", message = "ISBN 格式不正確")
	private String isbn;

	private Long authorId;

	@Min(value = 1450, message = "出版年不能早於 1450 年")
	@Max(value = 2100, message = "出版年不能晚於 2100 年")
	private Integer publishedYear;

	@DecimalMin(value = "0.0", message = "預估金額不能為負數")
	private BigDecimal estimatedPrice;
}
