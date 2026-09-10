package com.example.jsppractice.dto;

import java.time.LocalDateTime;

import com.example.jsppractice.model.BookRequestItem;
import com.example.jsppractice.model.ProcurementStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcurementSummary {
	private Long id;

	private ProcurementStatus status;

	private Long procuredBy;

	private LocalDateTime procuredAt;

	private Long bookId;

	private BookRequestItem bookRequestItem;
}
