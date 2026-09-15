package com.example.jsppractice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 手動觸發（ReconciliationController 的 /reconciliations/book-request-items、
// /reconciliations/procurement-items）保留給需要立即重跑的情況，這裡是額外加上的自動排程，
// 模擬銀行/保險常見的夜間批次（日終結帳）時段。對帳本身是純新增記錄、不會清掉舊資料，
// 排程重複跑或跟手動觸發前後夾雜都不會互相干擾。
@Component
public class ReconciliationScheduler {

	private static final Logger log = LoggerFactory.getLogger(ReconciliationScheduler.class);

	private final ReconciliationService reconciliationService;

	public ReconciliationScheduler(ReconciliationService reconciliationService) {
		this.reconciliationService = reconciliationService;
	}

	// cron 格式：秒 分 時 日 月 星期。"0 0 2 * * *" = 每天 02:00:00。
	@Scheduled(cron = "0 0 2 * * *")
	public void reconcileAll() {
		log.info("排程對帳開始");
		reconciliationService.reconcileBookRequestItems();
		reconciliationService.reconcileProcurementItems();
		log.info("排程對帳結束");
	}
}
