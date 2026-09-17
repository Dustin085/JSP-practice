package com.example.jsppractice.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.example.jsppractice.dto.DeliveryImportResult;
import com.example.jsppractice.sftp.DeliveryFileFetcher;

// 沒有標 @Component：這個 bean 要不要存在（要不要真的去連 SFTP）是 SftpConfig 決定的，
// 用 @Bean 方法註冊，不讓 RootConfig 的 component-scan 直接掃到。這樣沒有把 SftpConfig
// 一起載入的測試（大多數只載入 RootConfig 的既有測試）就不會因為找不到 DeliveryFileFetcher
// 這個 bean 而啟動失敗——SFTP 這塊要接真正的網路服務，本來就不該是每個測試都被迫背負的相依。
public class DeliveryImportScheduler {

	private static final Logger log = LoggerFactory.getLogger(DeliveryImportScheduler.class);

	private final DeliveryFileFetcher deliveryFileFetcher;
	private final DeliveryImportService deliveryImportService;

	public DeliveryImportScheduler(DeliveryFileFetcher deliveryFileFetcher,
			DeliveryImportService deliveryImportService) {
		this.deliveryFileFetcher = deliveryFileFetcher;
		this.deliveryImportService = deliveryImportService;
	}

	// 排在對帳排程（02:00）之前一小時：先讓到貨清單把 procurement_items 更新好，
	// 對帳那邊才不會撈到理應已經處理掉、卻還沒處理的資料。
	@Scheduled(cron = "0 0 1 * * *")
	public void importPendingDeliveries() {
		log.info("到貨清單排程開始");
		List<String> fileNames = deliveryFileFetcher.listPendingFileNames();
		for (String fileName : fileNames) {
			try {
				byte[] fileBytes = deliveryFileFetcher.download(fileName);
				DeliveryImportResult result = deliveryImportService.importDeliveries(fileBytes);
				deliveryFileFetcher.markProcessed(fileName);
				log.info("到貨清單處理完成：file={}, applied={}, skipped={}", fileName, result.appliedCount(),
						result.skipped().size());
			} catch (Exception e) {
				// 單一檔案處理失敗不影響同批其他檔案，跟單筆 Detail 失敗不影響同批其他筆是同一個原則
				log.error("到貨清單處理失敗：file={}", fileName, e);
				deliveryFileFetcher.markFailed(fileName);
			}
		}
		log.info("到貨清單排程結束");
	}
}
