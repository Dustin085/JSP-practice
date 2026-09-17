package com.example.jsppractice.model;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.example.jsppractice.exception.InvalidDeliveryRecordException;

public sealed interface DeliveryRecord {
	record HeaderRecord(String vendorCode, LocalDate fileDate) implements DeliveryRecord {
		static HeaderRecord parse(byte[] bytes) {
			String recordType = new String(bytes, 0, 1, Charset.forName("Big5"));
			if (!recordType.equals("H")) {
				throw new InvalidDeliveryRecordException("Header 記錄別不正確，預期 \"H\"，實際為：" + recordType);
			}
			String vendorCode = new String(bytes, 1, 6, Charset.forName("Big5")).trim();
			String fileDateString = new String(bytes, 7, 8, Charset.forName("Big5"));
			LocalDate fileDate = LocalDate.parse(fileDateString, DateTimeFormatter.ofPattern("yyyyMMdd"));
			return new HeaderRecord(vendorCode, fileDate);
		};
	};

	record DetailRecord(Long referenceId, String isbn, int quantity, BigDecimal unitPrice, LocalDate deliveredDate,
			DeliveryStatusCode statusCode, String bookTitle, String remark) implements DeliveryRecord {
		static DetailRecord parse(byte[] bytes) {
			String recordType = new String(bytes, 0, 1, Charset.forName("Big5"));
			if (!recordType.equals("D")) {
				throw new InvalidDeliveryRecordException("Detail 記錄別不正確，預期 \"D\"，實際為：" + recordType);
			}
			Long referenceId = Long.valueOf(new String(bytes, 1, 10, Charset.forName("Big5")));
			String isbn = new String(bytes, 11, 20, Charset.forName("Big5")).trim();
			int quantity = Integer.parseInt(new String(bytes, 31, 4, Charset.forName("Big5")));
			long unitPriceRaw = Long.parseLong(new String(bytes, 35, 8, Charset.forName("Big5")));
			BigDecimal unitPrice = BigDecimal.valueOf(unitPriceRaw, 2);
			String deliveredDateString = new String(bytes, 43, 8, Charset.forName("Big5"));
			LocalDate deliveredDate = LocalDate.parse(deliveredDateString, DateTimeFormatter.ofPattern("yyyyMMdd"));
			String statusCodeRaw = new String(bytes, 51, 1, Charset.forName("Big5"));
			DeliveryStatusCode statusCode = switch (statusCodeRaw) {
			case "1" -> DeliveryStatusCode.DELIVERED;
			case "9" -> DeliveryStatusCode.CANCELLED;
			default -> throw new InvalidDeliveryRecordException("未知的狀態碼，預期 \"1\" 或 \"9\"，實際為：" + statusCodeRaw);
			};
			String bookTitle = new String(bytes, 52, 40, Charset.forName("Big5")).trim();
			String remark = new String(bytes, 92, 20, Charset.forName("Big5")).trim();
			return new DetailRecord(referenceId, isbn, quantity, unitPrice, deliveredDate, statusCode, bookTitle,
					remark);
		}
	};

	record TrailerRecord(int detailCount) implements DeliveryRecord {
		static TrailerRecord parse(byte[] bytes) {
			String recordType = new String(bytes, 0, 1, Charset.forName("Big5"));
			if (!recordType.equals("T")) {
				throw new InvalidDeliveryRecordException("Trailer 記錄別不正確，預期 \"T\"，實際為：" + recordType);
			}
			int detailCount = Integer.valueOf(new String(bytes, 1, 6, Charset.forName("Big5")));
			return new TrailerRecord(detailCount);
		}
	};

	static List<DeliveryRecord> parse(byte[] bytes) {
		List<byte[]> lines = new ArrayList<byte[]>();
		int lastFLInx = 0;
		for (int i = 0; i < bytes.length; i++) {
			byte b = bytes[i];
			if (b == 0x0A) {
				byte[] line = Arrays.copyOfRange(bytes, lastFLInx, i);
				lines.add(line);
				lastFLInx = i + 1;
			}
		}
		if (lastFLInx < bytes.length) {
			byte[] line = Arrays.copyOfRange(bytes, lastFLInx, bytes.length);
			lines.add(line);
		}

		return lines.stream().<DeliveryRecord>map(DeliveryRecord::parseLine).toList();
	}

	static void validate(List<DeliveryRecord> deliveryRecords) {
		long headerCount = deliveryRecords.stream().filter(record -> record instanceof HeaderRecord).count();
		if (headerCount != 1L) {
			throw new InvalidDeliveryRecordException("Header 記錄數量不正確，預期剛好 1 筆，實際為：" + headerCount);
		}
		List<DeliveryRecord> trailerRecords = deliveryRecords.stream()
				.filter(record -> record instanceof TrailerRecord)
				.toList();
		if (trailerRecords.size() != 1) {
			throw new InvalidDeliveryRecordException("Trailer 記錄數量不正確，預期剛好 1 筆，實際為：" + trailerRecords.size());
		}
		long detailCount = deliveryRecords.stream().filter(record -> record instanceof DetailRecord).count();
		TrailerRecord trailerRecord = (TrailerRecord) trailerRecords.get(0);
		if (detailCount != trailerRecord.detailCount()) {
			throw new InvalidDeliveryRecordException("Detail 筆數與 Trailer 宣告的筆數不符，Trailer 宣告："
					+ trailerRecord.detailCount() + "，實際為：" + detailCount);
		}
	}

	private static DeliveryRecord parseLine(byte[] lineBytes) {
		String recordType = new String(lineBytes, 0, 1, Charset.forName("Big5"));

		return switch (recordType) {
		case "H" -> HeaderRecord.parse(lineBytes);
		case "D" -> DetailRecord.parse(lineBytes);
		case "T" -> TrailerRecord.parse(lineBytes);
		default -> throw new InvalidDeliveryRecordException("未知的記錄別：" + recordType);
		};
	}
}
