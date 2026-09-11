package com.example.jsppractice.mapper;

import java.util.List;

import com.example.jsppractice.dto.ReconciliationSummary;
import com.example.jsppractice.model.Reconciliation;

public interface ReconciliationMapper {
	List<ReconciliationSummary> findSummary();

	void insert(Reconciliation reconciliation);
}
