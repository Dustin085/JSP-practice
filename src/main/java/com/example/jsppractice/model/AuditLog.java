package com.example.jsppractice.model;

import java.time.Instant;
import java.util.Map;

import com.example.jsppractice.util.DisplayTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
	private Long id;

	private Long userId;

	private Instant auditedAt;

	private AuditActionType action;

	private AuditEntityType entityType;

	private Long entityId;

	private Map<String, Object> detail; // {status:{oldValue: "PENDING", newValue: "APPROVED"}}

	public String getAuditedAtDisplay() {
		return DisplayTime.format(auditedAt);
	}
}
