package com.example.jsppractice.dto;

import java.time.Instant;
import java.util.Map;

import com.example.jsppractice.model.AuditActionType;
import com.example.jsppractice.model.AuditEntityType;
import com.example.jsppractice.model.AuditLog;
import com.example.jsppractice.util.DisplayJson;
import com.example.jsppractice.util.DisplayTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogSummary {
	private Long id;

	private String userEmail;

	private Instant auditedAt;

	private AuditActionType action;

	private AuditEntityType entityType;

	private Long entityId;

	private Map<String, Object> detail;

	public static AuditLogSummary from(AuditLog auditLog, String userEmail) {
		return AuditLogSummary.builder().id(auditLog.getId()).userEmail(userEmail)
				.auditedAt(auditLog.getAuditedAt()).action(auditLog.getAction()).entityType(auditLog.getEntityType())
				.entityId(auditLog.getEntityId()).detail(auditLog.getDetail()).build();
	}

	public String getAuditedAtDisplay() {
		return DisplayTime.format(auditedAt);
	}

	public String getDetailDisplay() {
		return DisplayJson.prettyPrint(detail);
	}
}
