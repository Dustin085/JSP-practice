package com.example.jsppractice.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.example.jsppractice.exception.InvalidDeliveryRecordException;
import com.example.jsppractice.model.DeliveryRecord.DetailRecord;
import com.example.jsppractice.model.DeliveryRecord.HeaderRecord;
import com.example.jsppractice.model.DeliveryRecord.TrailerRecord;

public class DeliveryRecordTest {

	private static final Charset BIG5 = Charset.forName("Big5");

	// AN 欄位：左靠右補空白，補到指定 byte 長度（不是字元長度——這是整個解析邏輯要驗證的重點）
	private static byte[] anField(String value, int length) {
		byte[] valueBytes = value.getBytes(BIG5);
		byte[] field = new byte[length];
		Arrays.fill(field, (byte) ' ');
		System.arraycopy(valueBytes, 0, field, 0, valueBytes.length);
		return field;
	}

	// N 欄位：右靠左補零，補到指定長度
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

	// 用 LF 把多行接成一個檔案，刻意不補最後一行的結尾換行符
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

	private static byte[] validHeaderLine() {
		return concat(anField("H", 1), anField("V00001", 6), nField(20260916, 8));
	}

	private static byte[] validDetailLine() {
		return concat(anField("D", 1), nField(123, 10), anField("9780134685991", 20), nField(10, 4),
				nField(12550, 8), nField(20260915, 8), anField("1", 1), anField("深入淺出設計模式", 40),
				anField("已確認", 20));
	}

	private static byte[] validTrailerLine() {
		return trailerLineWithCount(1);
	}

	private static byte[] trailerLineWithCount(int detailCount) {
		return concat(anField("T", 1), nField(detailCount, 6));
	}

	@Test
	public void parseHeaderExtractsVendorCodeAndFileDate() {
		HeaderRecord header = HeaderRecord.parse(validHeaderLine());

		assertEquals("V00001", header.vendorCode());
		assertEquals(LocalDate.of(2026, 9, 16), header.fileDate());
	}

	@Test(expected = InvalidDeliveryRecordException.class)
	public void parseHeaderThrowsWhenRecordTypeIsWrong() {
		byte[] line = validHeaderLine();
		line[0] = 'X';

		HeaderRecord.parse(line);
	}

	@Test
	public void parseDetailExtractsAllFieldsIncludingChineseTitle() {
		DetailRecord detail = DetailRecord.parse(validDetailLine());

		assertEquals(Long.valueOf(123), detail.referenceId());
		assertEquals("9780134685991", detail.isbn());
		assertEquals(10, detail.quantity());
		assertEquals(BigDecimal.valueOf(12550, 2), detail.unitPrice());
		assertEquals(LocalDate.of(2026, 9, 15), detail.deliveredDate());
		assertEquals(DeliveryStatusCode.DELIVERED, detail.statusCode());
		// 書名之後緊接著備註欄位，如果中文書名的 byte 長度算錯，備註會被切錯位置，
		// 這兩個欄位一起斷言才能真正驗證到「按 byte 不按字元切割」這件事
		assertEquals("深入淺出設計模式", detail.bookTitle());
		assertEquals("已確認", detail.remark());
	}

	@Test
	public void parseDetailMapsCancelledStatusCode() {
		byte[] line = validDetailLine();
		line[51] = '9';

		DetailRecord detail = DetailRecord.parse(line);

		assertEquals(DeliveryStatusCode.CANCELLED, detail.statusCode());
	}

	@Test(expected = InvalidDeliveryRecordException.class)
	public void parseDetailThrowsOnUnknownStatusCode() {
		byte[] line = validDetailLine();
		line[51] = '5';

		DetailRecord.parse(line);
	}

	@Test(expected = InvalidDeliveryRecordException.class)
	public void parseDetailThrowsWhenRecordTypeIsWrong() {
		byte[] line = validDetailLine();
		line[0] = 'X';

		DetailRecord.parse(line);
	}

	@Test
	public void parseTrailerExtractsDetailCount() {
		TrailerRecord trailer = TrailerRecord.parse(validTrailerLine());

		assertEquals(1, trailer.detailCount());
	}

	@Test(expected = InvalidDeliveryRecordException.class)
	public void parseTrailerThrowsWhenRecordTypeIsWrong() {
		byte[] line = validTrailerLine();
		line[0] = 'X';

		TrailerRecord.parse(line);
	}

	@Test
	public void parseFileReturnsAllRecordsInOrderWhenLastLineHasNoTrailingNewline() {
		byte[] file = joinLines(validHeaderLine(), validDetailLine(), validTrailerLine());

		List<DeliveryRecord> records = DeliveryRecord.parse(file);

		assertEquals(3, records.size());
		assertTrue(records.get(0) instanceof HeaderRecord);
		assertTrue(records.get(1) instanceof DetailRecord);
		assertTrue(records.get(2) instanceof TrailerRecord);
		// 特別驗證最後一行（沒有結尾換行符的 Trailer）沒有被截斷最後一個 byte
		TrailerRecord trailer = (TrailerRecord) records.get(2);
		assertEquals(1, trailer.detailCount());
	}

	@Test
	public void parseFileReturnsAllRecordsWhenFileEndsWithTrailingNewline() {
		byte[] file = concat(joinLines(validHeaderLine(), validDetailLine(), validTrailerLine()),
				new byte[] { 0x0A });

		List<DeliveryRecord> records = DeliveryRecord.parse(file);

		assertEquals(3, records.size());
	}

	@Test(expected = InvalidDeliveryRecordException.class)
	public void parseFileThrowsWhenALineHasUnknownRecordType() {
		byte[] badLine = validDetailLine();
		badLine[0] = 'X';
		byte[] file = joinLines(validHeaderLine(), badLine, validTrailerLine());

		DeliveryRecord.parse(file);
	}

	@Test
	public void validateDoesNotThrowForWellFormedFile() {
		byte[] file = joinLines(validHeaderLine(), validDetailLine(), validDetailLine(), trailerLineWithCount(2));

		DeliveryRecord.validate(DeliveryRecord.parse(file));
	}

	@Test(expected = InvalidDeliveryRecordException.class)
	public void validateThrowsWhenHeaderIsMissing() {
		byte[] file = joinLines(validDetailLine(), validTrailerLine());

		DeliveryRecord.validate(DeliveryRecord.parse(file));
	}

	@Test(expected = InvalidDeliveryRecordException.class)
	public void validateThrowsWhenThereAreTwoHeaders() {
		byte[] file = joinLines(validHeaderLine(), validHeaderLine(), validDetailLine(), validTrailerLine());

		DeliveryRecord.validate(DeliveryRecord.parse(file));
	}

	@Test(expected = InvalidDeliveryRecordException.class)
	public void validateThrowsWhenTrailerIsMissing() {
		byte[] file = joinLines(validHeaderLine(), validDetailLine());

		DeliveryRecord.validate(DeliveryRecord.parse(file));
	}

	@Test(expected = InvalidDeliveryRecordException.class)
	public void validateThrowsWhenThereAreTwoTrailers() {
		byte[] file = joinLines(validHeaderLine(), validDetailLine(), validTrailerLine(), validTrailerLine());

		DeliveryRecord.validate(DeliveryRecord.parse(file));
	}

	@Test(expected = InvalidDeliveryRecordException.class)
	public void validateThrowsWhenDetailCountDoesNotMatchTrailer() {
		byte[] file = joinLines(validHeaderLine(), validDetailLine(), trailerLineWithCount(2));

		DeliveryRecord.validate(DeliveryRecord.parse(file));
	}
}
