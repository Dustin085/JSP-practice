package com.example.jsppractice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.jsppractice.service.DeliveryImportScheduler;
import com.example.jsppractice.service.DeliveryImportService;
import com.example.jsppractice.sftp.DeliveryFileFetcher;
import com.example.jsppractice.sftp.SftpDeliveryFileFetcher;

// 連線資訊一律走環境變數，跟 AesEncryptor 的金鑰讀取是同一套慣例：正式環境一定要設這些環境變數，
// 沒設就退回明確標記「僅供本機開發」的預設值（連本機 Windows OpenSSH Server），方便本機起 Tomcat
// 不用額外設定就能跑。
@Configuration
public class SftpConfig {

	private static final String DEV_ONLY_DEFAULT_HOST = "localhost";
	private static final String DEV_ONLY_DEFAULT_PORT = "22";
	private static final String DEV_ONLY_DEFAULT_USERNAME = "dev-only-default-username";
	private static final String DEV_ONLY_DEFAULT_PASSWORD = "dev-only-insecure-default-password";
	private static final String DEV_ONLY_DEFAULT_REMOTE_DIR = "/delivery-notices";

	@Bean
	public DeliveryFileFetcher deliveryFileFetcher() {
		String host = getEnvOrDefault("SFTP_HOST", DEV_ONLY_DEFAULT_HOST);
		int port = Integer.parseInt(getEnvOrDefault("SFTP_PORT", DEV_ONLY_DEFAULT_PORT));
		String username = getEnvOrDefault("SFTP_USERNAME", DEV_ONLY_DEFAULT_USERNAME);
		String password = getEnvOrDefault("SFTP_PASSWORD", DEV_ONLY_DEFAULT_PASSWORD);
		String remoteDir = getEnvOrDefault("SFTP_REMOTE_DIR", DEV_ONLY_DEFAULT_REMOTE_DIR);
		return new SftpDeliveryFileFetcher(host, port, username, password, remoteDir);
	}

	@Bean
	public DeliveryImportScheduler deliveryImportScheduler(DeliveryFileFetcher deliveryFileFetcher,
			DeliveryImportService deliveryImportService) {
		return new DeliveryImportScheduler(deliveryFileFetcher, deliveryImportService);
	}

	private static String getEnvOrDefault(String name, String defaultValue) {
		String value = System.getenv(name);
		return (value == null || value.isBlank()) ? defaultValue : value;
	}
}
