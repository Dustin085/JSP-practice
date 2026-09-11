package com.example.jsppractice.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationItem {
	private Long id;

	private Long reconciliationId;

	private AuditEntityType entityType;

	private Long entityId;

	private ReconciliationDiscrepancyType discrepancyType;

	private Map<String, Object> detail; // {reason: "找不到對應的 procurement_item"}
}
