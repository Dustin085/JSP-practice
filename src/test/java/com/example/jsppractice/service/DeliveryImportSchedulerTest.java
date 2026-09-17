package com.example.jsppractice.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.example.jsppractice.dto.DeliveryFileImportOutcome;
import com.example.jsppractice.dto.DeliveryImportResult;
import com.example.jsppractice.exception.SftpOperationException;
import com.example.jsppractice.sftp.DeliveryFileFetcher;

@RunWith(MockitoJUnitRunner.class)
public class DeliveryImportSchedulerTest {

	@Mock
	private DeliveryFileFetcher deliveryFileFetcher;

	@Mock
	private DeliveryImportService deliveryImportService;

	@InjectMocks
	private DeliveryImportScheduler deliveryImportScheduler;

	@Test
	public void importPendingDeliveriesMarksFileProcessedOnSuccess() {
		when(deliveryFileFetcher.listPendingFileNames()).thenReturn(List.of("20260917.txt"));
		byte[] fileBytes = "content".getBytes();
		when(deliveryFileFetcher.download("20260917.txt")).thenReturn(fileBytes);
		DeliveryImportResult result = new DeliveryImportResult(1, List.of());
		when(deliveryImportService.importDeliveries(fileBytes)).thenReturn(result);

		List<DeliveryFileImportOutcome> outcomes = deliveryImportScheduler.importPendingDeliveries();

		assertEquals(1, outcomes.size());
		assertTrue(outcomes.get(0).isSucceeded());
		assertEquals(result, outcomes.get(0).result());
		verify(deliveryFileFetcher).markProcessed("20260917.txt");
	}

	@Test
	public void importPendingDeliveriesMarksFileFailedWhenImportThrows() {
		when(deliveryFileFetcher.listPendingFileNames()).thenReturn(List.of("bad.txt"));
		when(deliveryFileFetcher.download("bad.txt")).thenReturn("bad".getBytes());
		when(deliveryImportService.importDeliveries(any())).thenThrow(new RuntimeException("筆數控管失敗"));

		List<DeliveryFileImportOutcome> outcomes = deliveryImportScheduler.importPendingDeliveries();

		assertEquals(1, outcomes.size());
		assertFalse(outcomes.get(0).isSucceeded());
		assertEquals("筆數控管失敗", outcomes.get(0).errorMessage());
		verify(deliveryFileFetcher).markFailed("bad.txt");
		verify(deliveryFileFetcher, never()).markProcessed(any());
	}

	@Test
	public void importPendingDeliveriesProcessesEachFileIndependently() {
		when(deliveryFileFetcher.listPendingFileNames()).thenReturn(List.of("ok.txt", "bad.txt"));
		when(deliveryFileFetcher.download("ok.txt")).thenReturn("ok".getBytes());
		when(deliveryFileFetcher.download("bad.txt")).thenReturn("bad".getBytes());
		when(deliveryImportService.importDeliveries("ok".getBytes())).thenReturn(new DeliveryImportResult(1, List.of()));
		doThrow(new RuntimeException("解析失敗")).when(deliveryImportService).importDeliveries("bad".getBytes());

		List<DeliveryFileImportOutcome> outcomes = deliveryImportScheduler.importPendingDeliveries();

		assertEquals(2, outcomes.size());
		assertTrue(outcomes.get(0).isSucceeded());
		assertFalse(outcomes.get(1).isSucceeded());
	}

	@Test
	public void importPendingDeliveriesReturnsEmptyListWhenNoPendingFiles() {
		when(deliveryFileFetcher.listPendingFileNames()).thenReturn(List.of());

		List<DeliveryFileImportOutcome> outcomes = deliveryImportScheduler.importPendingDeliveries();

		assertEquals(0, outcomes.size());
	}

	@Test(expected = SftpOperationException.class)
	public void importPendingDeliveriesLetsListingFailureThrow() {
		// 連線本身失敗（不是單一檔案處理失敗）沒有檔案可以標記，直接讓例外往外冒，
		// 由呼叫端（排程本身或手動觸發的 controller）決定怎麼處理連線層級的錯誤
		when(deliveryFileFetcher.listPendingFileNames()).thenThrow(new SftpOperationException("連線失敗", null));

		deliveryImportScheduler.importPendingDeliveries();
	}
}
