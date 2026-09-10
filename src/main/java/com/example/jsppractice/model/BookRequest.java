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
public class BookRequest {

	private Long id;

	private Long requesterId;

	private Long approverId;

	private BookRequestStatus status;

	private Instant requestedAt;

	private Instant approvedAt;

	private String idempotencyKey;
}
