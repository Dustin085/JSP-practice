package com.example.jsppractice.service;

import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReconciliationSchedulerTest {

	@Mock
	private ReconciliationService reconciliationService;

	@InjectMocks
	private ReconciliationScheduler reconciliationScheduler;

	@Test
	public void reconcileAllRunsBothReconciliations() {
		reconciliationScheduler.reconcileAll();

		verify(reconciliationService).reconcileBookRequestItems();
		verify(reconciliationService).reconcileProcurementItems();
	}
}
