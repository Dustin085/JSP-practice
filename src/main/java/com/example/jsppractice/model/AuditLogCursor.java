package com.example.jsppractice.model;

import java.time.Instant;

public record AuditLogCursor(Instant auditedAt, Long id) {
}
