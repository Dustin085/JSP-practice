package com.example.jsppractice.service;

import java.util.List;

import com.example.jsppractice.dto.ReconciliationSummary;
import com.example.jsppractice.model.Reconciliation;

public interface ReconciliationService {
	Reconciliation reconcileBookRequestItems();

	Reconciliation reconcileProcurementItems();

	List<ReconciliationSummary> findSummary();
}
