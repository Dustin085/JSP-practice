package com.example.jsppractice.dto;

import java.time.Instant;
import java.util.List;

import com.example.jsppractice.model.ReconciliationItem;
import com.example.jsppractice.model.ReconciliationStatus;
import com.example.jsppractice.model.ReconciliationType;
import com.example.jsppractice.util.DisplayTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationSummary {
	private Long id;

	private Instant reconciledAt;

	private ReconciliationType reconciliationType;

	private ReconciliationStatus status;

	private List<ReconciliationItem> reconciliationItems;

	public String getReconciledAtDisplay() {
		return DisplayTime.format(reconciledAt);
	}
}
