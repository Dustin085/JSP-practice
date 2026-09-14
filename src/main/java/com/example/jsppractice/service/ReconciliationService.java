package com.example.jsppractice.service;

import java.util.List;

import com.example.jsppractice.dto.ReconciliationSummary;
import com.example.jsppractice.model.Book;
import com.example.jsppractice.model.Reconciliation;
import com.example.jsppractice.model.User;

public interface ReconciliationService {
	Reconciliation reconcileBookRequestItems();

	Reconciliation reconcileProcurementItems();

	List<ReconciliationSummary> findSummary();

	void resolve(Long reconciliationItemId, User currentUser);

	void writeOff(Long reconciliationItemId, User currentUser);

	void backfillProcurementItem(Long reconciliationItemId, User currentUser);

	List<Book> findRelinkCandidates(Long reconciliationItemId);

	void relink(Long reconciliationItemId, Long bookId, User currentUser);

	void revertToPending(Long reconciliationItemId, User currentUser);
}
