package com.example.jsppractice.service;

import com.example.jsppractice.dto.DeliveryImportResult;

public interface DeliveryImportService {
	DeliveryImportResult importDeliveries(byte[] fileBytes);
}
