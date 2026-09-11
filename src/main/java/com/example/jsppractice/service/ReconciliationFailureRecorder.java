package com.example.jsppractice.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.jsppractice.mapper.ReconciliationMapper;
import com.example.jsppractice.model.Reconciliation;
import com.example.jsppractice.model.ReconciliationStatus;
import com.example.jsppractice.model.ReconciliationType;

// 獨立成一個 bean，是因為呼叫端 (ReconciliationServiceImpl) catch 例外時，
// 自己所在的交易已經失敗、即將 rollback；如果這個方法跟呼叫端在同一個 bean 裡，
// self-invocation 會繞過 Spring proxy，REQUIRES_NEW 不會生效。
// 獨立成另一個 bean、透過注入呼叫，才能真的開一個新交易把這筆失敗紀錄留下來。
@Service
public class ReconciliationFailureRecorder {

	private final ReconciliationMapper reconciliationMapper;

	public ReconciliationFailureRecorder(ReconciliationMapper reconciliationMapper) {
		this.reconciliationMapper = reconciliationMapper;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Reconciliation recordFailure(ReconciliationType reconciliationType) {
		Reconciliation reconciliation = Reconciliation.builder()
				.reconciledAt(Instant.now())
				.reconciliationType(reconciliationType)
				.status(ReconciliationStatus.FAILED)
				.build();
		reconciliationMapper.insert(reconciliation);
		return reconciliation;
	}
}
