package com.example.jsppractice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.jsppractice.dto.DeliveryImportResult;
import com.example.jsppractice.dto.DeliveryImportSkip;
import com.example.jsppractice.exception.ProcurementAlreadyCompletedException;
import com.example.jsppractice.model.DeliveryRecord;
import com.example.jsppractice.model.DeliveryRecord.DetailRecord;
import com.example.jsppractice.model.DeliveryStatusCode;
import com.example.jsppractice.model.User;

@Service
public class DeliveryImportServiceImpl implements DeliveryImportService {
	private static final Logger log = LoggerFactory.getLogger(DeliveryImportServiceImpl.class);

	private final ProcurementService procurementService;
	private final UserService userService;

	public DeliveryImportServiceImpl(ProcurementService procurementService, UserService userService) {
		this.procurementService = procurementService;
		this.userService = userService;
	}

	@Override
	@Transactional
	public DeliveryImportResult importDeliveries(byte[] fileBytes) {
		List<DeliveryRecord> records = DeliveryRecord.parse(fileBytes);
		DeliveryRecord.validate(records);

		User systemUser = userService.findByEmail(SystemAccount.EMAIL)
				.orElseThrow(() -> new IllegalStateException("找不到系統帳號：" + SystemAccount.EMAIL));

		int appliedCount = 0;
		List<DeliveryImportSkip> skipped = new ArrayList<>();
		for (DeliveryRecord record : records) {
			if (!(record instanceof DetailRecord detail)) {
				continue;
			}
			Optional<DeliveryImportSkip> skip = applyDetail(detail, systemUser);
			if (skip.isPresent()) {
				skipped.add(skip.get());
			} else {
				appliedCount++;
			}
		}

		log.info("到貨清單匯入完成：applied={}, skipped={}", appliedCount, skipped.size());
		return new DeliveryImportResult(appliedCount, skipped);
	}

	// 單筆異常不影響同批其他筆——回傳略過原因，而不是丟例外中斷整批處理。
	// 完成邏輯直接重用 ProcurementService.completeProcurement()：查無項目／已完成過都會丟例外，
	// 用系統帳號當 currentUser，跟人工按「完成採購」走的是同一套稽核紀錄
	private Optional<DeliveryImportSkip> applyDetail(DetailRecord detail, User systemUser) {
		if (detail.statusCode() == DeliveryStatusCode.CANCELLED) {
			return Optional.of(new DeliveryImportSkip(detail.referenceId(), "廠商回報缺貨/取消，不轉為已完成"));
		}

		try {
			procurementService.completeProcurement(detail.referenceId(), systemUser);
			log.info("到貨清單匯入：採購項目已完成，procurementItemId={}, isbn={}", detail.referenceId(), detail.isbn());
			return Optional.empty();
		} catch (NoSuchElementException | ProcurementAlreadyCompletedException e) {
			return Optional.of(new DeliveryImportSkip(detail.referenceId(), e.getMessage()));
		}
	}
}
