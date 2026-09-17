package com.example.jsppractice.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.example.jsppractice.dto.DeliveryImportResult;
import com.example.jsppractice.exception.ProcurementAlreadyCompletedException;
import com.example.jsppractice.model.User;

@RunWith(MockitoJUnitRunner.class)
public class DeliveryImportServiceImplTest {

	@Mock
	private ProcurementService procurementService;

	@Mock
	private UserService userService;

	@InjectMocks
	private DeliveryImportServiceImpl deliveryImportService;

	private static final User SYSTEM_USER = User.builder().id(1L).email(SystemAccount.EMAIL).build();

	private static final Charset BIG5 = Charset.forName("Big5");

	private static byte[] anField(String value, int length) {
		byte[] valueBytes = value.getBytes(BIG5);
		byte[] field = new byte[length];
		Arrays.fill(field, (byte) ' ');
		System.arraycopy(valueBytes, 0, field, 0, valueBytes.length);
		return field;
	}

	private static byte[] nField(long value, int length) {
		String digits = String.format("%0" + length + "d", value);
		return digits.getBytes(BIG5);
	}

	private static byte[] concat(byte[]... parts) {
		int totalLength = 0;
		for (byte[] part : parts) {
			totalLength += part.length;
		}
		byte[] result = new byte[totalLength];
		int offset = 0;
		for (byte[] part : parts) {
			System.arraycopy(part, 0, result, offset, part.length);
			offset += part.length;
		}
		return result;
	}

	private static byte[] joinLines(byte[]... lines) {
		int totalLength = 0;
		for (int i = 0; i < lines.length; i++) {
			totalLength += lines[i].length;
			if (i < lines.length - 1) {
				totalLength += 1;
			}
		}
		byte[] result = new byte[totalLength];
		int offset = 0;
		for (int i = 0; i < lines.length; i++) {
			System.arraycopy(lines[i], 0, result, offset, lines[i].length);
			offset += lines[i].length;
			if (i < lines.length - 1) {
				result[offset] = 0x0A;
				offset++;
			}
		}
		return result;
	}

	private static byte[] headerLine() {
		return concat(anField("H", 1), anField("V00001", 6), nField(20260916, 8));
	}

	private static byte[] trailerLine(int detailCount) {
		return concat(anField("T", 1), nField(detailCount, 6));
	}

	// statusCode: '1' = 正常到貨, '9' = 缺貨/取消
	private static byte[] detailLine(long referenceId, char statusCode) {
		return concat(anField("D", 1), nField(referenceId, 10), anField("9780134685991", 20), nField(10, 4),
				nField(12550, 8), nField(20260915, 8), anField(String.valueOf(statusCode), 1),
				anField("深入淺出設計模式", 40), anField("", 20));
	}

	private static byte[] fileWith(byte[]... detailLines) {
		byte[][] allLines = new byte[detailLines.length + 2][];
		allLines[0] = headerLine();
		System.arraycopy(detailLines, 0, allLines, 1, detailLines.length);
		allLines[allLines.length - 1] = trailerLine(detailLines.length);
		return joinLines(allLines);
	}

	@Test
	public void importDeliveriesCompletesProcurementItemWhenPending() {
		when(userService.findByEmail(SystemAccount.EMAIL)).thenReturn(Optional.of(SYSTEM_USER));

		DeliveryImportResult result = deliveryImportService.importDeliveries(fileWith(detailLine(123, '1')));

		assertEquals(1, result.appliedCount());
		assertTrue(result.skipped().isEmpty());
		verify(procurementService).completeProcurement(123L, SYSTEM_USER);
	}

	@Test
	public void importDeliveriesSkipsWhenProcurementItemNotFound() {
		when(userService.findByEmail(SystemAccount.EMAIL)).thenReturn(Optional.of(SYSTEM_USER));
		doThrow(new NoSuchElementException("找不到採購清單")).when(procurementService).completeProcurement(999L,
				SYSTEM_USER);

		DeliveryImportResult result = deliveryImportService.importDeliveries(fileWith(detailLine(999, '1')));

		assertEquals(0, result.appliedCount());
		assertEquals(1, result.skipped().size());
		assertEquals(Long.valueOf(999L), result.skipped().get(0).referenceId());
		assertEquals("找不到採購清單", result.skipped().get(0).reason());
	}

	@Test
	public void importDeliveriesSkipsWhenProcurementItemAlreadyCompleted() {
		when(userService.findByEmail(SystemAccount.EMAIL)).thenReturn(Optional.of(SYSTEM_USER));
		doThrow(new ProcurementAlreadyCompletedException("無法修改已完成的採購清單")).when(procurementService)
				.completeProcurement(123L, SYSTEM_USER);

		DeliveryImportResult result = deliveryImportService.importDeliveries(fileWith(detailLine(123, '1')));

		assertEquals(0, result.appliedCount());
		assertEquals(1, result.skipped().size());
		assertEquals("無法修改已完成的採購清單", result.skipped().get(0).reason());
	}

	@Test
	public void importDeliveriesSkipsCancelledStatusCodeWithoutCallingProcurementService() {
		when(userService.findByEmail(SystemAccount.EMAIL)).thenReturn(Optional.of(SYSTEM_USER));

		DeliveryImportResult result = deliveryImportService.importDeliveries(fileWith(detailLine(123, '9')));

		assertEquals(0, result.appliedCount());
		assertEquals(1, result.skipped().size());
		assertEquals("廠商回報缺貨/取消，不轉為已完成", result.skipped().get(0).reason());
		verify(procurementService, never()).completeProcurement(any(), any());
	}

	@Test
	public void importDeliveriesProcessesEachDetailIndependently() {
		when(userService.findByEmail(SystemAccount.EMAIL)).thenReturn(Optional.of(SYSTEM_USER));
		doThrow(new NoSuchElementException("找不到採購清單")).when(procurementService).completeProcurement(999L,
				SYSTEM_USER);

		DeliveryImportResult result = deliveryImportService
				.importDeliveries(fileWith(detailLine(123, '1'), detailLine(999, '1')));

		assertEquals(1, result.appliedCount());
		assertEquals(1, result.skipped().size());
		assertEquals(Long.valueOf(999L), result.skipped().get(0).referenceId());
	}

	@Test(expected = IllegalStateException.class)
	public void importDeliveriesThrowsWhenSystemAccountIsMissing() {
		when(userService.findByEmail(SystemAccount.EMAIL)).thenReturn(Optional.empty());

		deliveryImportService.importDeliveries(fileWith(detailLine(123, '1')));
	}

	@Test(expected = com.example.jsppractice.exception.InvalidDeliveryRecordException.class)
	public void importDeliveriesThrowsWhenTrailerCountDoesNotMatchActualDetailCount() {
		// 檔案結構本身有問題，validate() 在查系統帳號之前就先丟例外，這裡刻意不 stub userService
		byte[] file = joinLines(headerLine(), detailLine(123, '1'), trailerLine(2));

		deliveryImportService.importDeliveries(file);
	}
}
