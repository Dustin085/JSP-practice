package com.example.jsppractice.dto;

import java.util.List;

public record DeliveryImportResult(int appliedCount, List<DeliveryImportSkip> skipped) {
}
