package com.example.jsppractice.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.jsppractice.model.BookRequest;
import com.example.jsppractice.model.BookRequestItem;
import com.example.jsppractice.model.BookRequestStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookRequestSummary {
	private Long id;

	private Long requesterId;

	private Long approverId;

	private BookRequestStatus status;

	private LocalDateTime requestedAt;

	private LocalDateTime approvedAt;

	private List<BookRequestItem> bookRequestItems;

	public static BookRequestSummary from(BookRequest bookRequest, List<BookRequestItem> bookRequestItems) {
		return BookRequestSummary.builder().id(bookRequest.getId()).requesterId(bookRequest.getRequesterId())
				.approverId(bookRequest.getApproverId()).status(bookRequest.getStatus())
				.requestedAt(bookRequest.getRequestedAt()).approvedAt(bookRequest.getApprovedAt())
				.bookRequestItems(bookRequestItems).build();
	}
}
