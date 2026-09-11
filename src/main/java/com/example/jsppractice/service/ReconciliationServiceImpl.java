package com.example.jsppractice.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.jsppractice.dto.ReconciliationSummary;
import com.example.jsppractice.mapper.ReconciliationMapper;
import com.example.jsppractice.model.Reconciliation;
import com.example.jsppractice.model.ReconciliationType;

@Service
public class ReconciliationServiceImpl implements ReconciliationService {
	private static final Logger log = LoggerFactory.getLogger(ReconciliationServiceImpl.class);

	private final ReconciliationChecker reconciliationChecker;
	private final ReconciliationFailureRecorder reconciliationFailureRecorder;
	private final ReconciliationMapper reconciliationMapper;

	public ReconciliationServiceImpl(ReconciliationChecker reconciliationChecker,
			ReconciliationFailureRecorder reconciliationFailureRecorder, ReconciliationMapper reconciliationMapper) {
		this.reconciliationChecker = reconciliationChecker;
		this.reconciliationFailureRecorder = reconciliationFailureRecorder;
		this.reconciliationMapper = reconciliationMapper;
	}

	@Override
	public Reconciliation reconcileBookRequestItems() {
		try {
			return reconciliationChecker.checkBookRequestItems();
		} catch (Exception e) {
			log.error("對帳失敗：type={}", ReconciliationType.BOOK_REQUEST_PROCUREMENT, e);
			return reconciliationFailureRecorder.recordFailure(ReconciliationType.BOOK_REQUEST_PROCUREMENT);
		}
	}

	@Override
	public Reconciliation reconcileProcurementItems() {
		try {
			return reconciliationChecker.checkProcurementItems();
		} catch (Exception e) {
			log.error("對帳失敗：type={}", ReconciliationType.PROCUREMENT_BOOK, e);
			return reconciliationFailureRecorder.recordFailure(ReconciliationType.PROCUREMENT_BOOK);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<ReconciliationSummary> findSummary() {
		return reconciliationMapper.findSummary();
	}
}
