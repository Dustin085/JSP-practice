package com.example.jsppractice.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookRequest {

	private Long id;

	private Long requesterId;

	private Long approverId;

	private BookRequestStatus status;

	private LocalDateTime requestedAt;

	private LocalDateTime approvedAt;

	private String idempotencyKey;
}
