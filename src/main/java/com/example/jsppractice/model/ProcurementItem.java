package com.example.jsppractice.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcurementItem {

	private Long id;

	private Long bookRequestItemId;

	private ProcurementStatus status;

	private Long procuredBy;

	private Instant procuredAt;

	private Long bookId;

	public static ProcurementItem from(BookRequestItem bookRequestItem) {
		return ProcurementItem.builder()
				.bookRequestItemId(bookRequestItem.getId())
				.status(ProcurementStatus.PENDING)
				.build();
	}
}
