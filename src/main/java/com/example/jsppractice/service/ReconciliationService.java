package com.example.jsppractice.service;

import com.example.jsppractice.model.Reconciliation;

public interface ReconciliationService {
	Reconciliation reconcileBookRequestItems();

	Reconciliation reconcileProcurementItems();
}
