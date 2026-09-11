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
public class Reconciliation {
	private Long id;

	private Instant reconciledAt;

	private ReconciliationType reconciliationType;

	private ReconciliationStatus status;
}
