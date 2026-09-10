package com.example.jsppractice.service;

import java.util.List;

import com.example.jsppractice.dto.ProcurementSummary;
import com.example.jsppractice.model.ProcurementItem;
import com.example.jsppractice.model.ProcurementStatus;
import com.example.jsppractice.model.User;

public interface ProcurementService {
	ProcurementItem completeProcurement(Long procurementItemId, User currentUser);

	List<ProcurementItem> findByStatus(ProcurementStatus status);

	List<ProcurementSummary> findSummary(ProcurementStatus status);
}
