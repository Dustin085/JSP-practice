package com.example.jsppractice.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.example.jsppractice.mapper.ReconciliationMapper;
import com.example.jsppractice.model.Reconciliation;
import com.example.jsppractice.model.ReconciliationStatus;
import com.example.jsppractice.model.ReconciliationType;

@RunWith(MockitoJUnitRunner.class)
public class ReconciliationServiceImplTest {

	@Mock
	private ReconciliationChecker reconciliationChecker;

	@Mock
	private ReconciliationFailureRecorder reconciliationFailureRecorder;

	@Mock
	private ReconciliationMapper reconciliationMapper;

	@InjectMocks
	private ReconciliationServiceImpl reconciliationService;

	@Test
	public void reconcileBookRequestItemsReturnsCheckerResultOnSuccess() {
		Reconciliation success = Reconciliation.builder().id(1L)
				.reconciliationType(ReconciliationType.BOOK_REQUEST_PROCUREMENT)
				.status(ReconciliationStatus.COMPLETED_NO_DISCREPANCY).build();
		when(reconciliationChecker.checkBookRequestItems()).thenReturn(success);

		Reconciliation result = reconciliationService.reconcileBookRequestItems();

		assertEquals(success, result);
		verify(reconciliationFailureRecorder, never()).recordFailure(org.mockito.ArgumentMatchers.any());
	}

	@Test
	public void reconcileBookRequestItemsDelegatesToFailureRecorderWhenCheckerThrows() {
		when(reconciliationChecker.checkBookRequestItems()).thenThrow(new RuntimeException("DB 掛了"));
		Reconciliation failure = Reconciliation.builder().id(2L)
				.reconciliationType(ReconciliationType.BOOK_REQUEST_PROCUREMENT).status(ReconciliationStatus.FAILED)
				.build();
		when(reconciliationFailureRecorder.recordFailure(ReconciliationType.BOOK_REQUEST_PROCUREMENT))
				.thenReturn(failure);

		Reconciliation result = reconciliationService.reconcileBookRequestItems();

		assertEquals(ReconciliationStatus.FAILED, result.getStatus());
		verify(reconciliationFailureRecorder).recordFailure(ReconciliationType.BOOK_REQUEST_PROCUREMENT);
	}

	@Test
	public void reconcileProcurementItemsDelegatesToFailureRecorderWhenCheckerThrows() {
		when(reconciliationChecker.checkProcurementItems()).thenThrow(new RuntimeException("DB 掛了"));
		Reconciliation failure = Reconciliation.builder().id(3L)
				.reconciliationType(ReconciliationType.PROCUREMENT_BOOK).status(ReconciliationStatus.FAILED).build();
		when(reconciliationFailureRecorder.recordFailure(ReconciliationType.PROCUREMENT_BOOK)).thenReturn(failure);

		Reconciliation result = reconciliationService.reconcileProcurementItems();

		assertEquals(ReconciliationStatus.FAILED, result.getStatus());
		verify(reconciliationFailureRecorder).recordFailure(ReconciliationType.PROCUREMENT_BOOK);
	}
}
