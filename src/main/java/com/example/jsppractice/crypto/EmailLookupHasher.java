package com.example.jsppractice.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

// email 欄位被 AesEncryptor 加密之後，WHERE email = ? 沒辦法用了：AES/GCM 每次加密都用新的隨機 IV，
// 同一個 email 加密兩次密文完全不一樣。這裡是「盲索引（blind index）」：用另一把跟 AES 分開的密鑰
// 對 email 算 HMAC-SHA256——同一個輸入永遠得到同一個輸出（確定性），但沒有這把密鑰猜不出原文
// （跟裸的 SHA-256 不一樣，email 這種格式熵不高，沒有密鑰保護的雜湊值可以被字典/彩虹表反推）。
// 登入查找改成查這個雜湊欄位，真正的 email 明文只在需要顯示/使用時才透過 AesEncryptor 解密拿出來。
public final class EmailLookupHasher {

	private static final String ALGORITHM = "HmacSHA256";

	// 刻意跟 AesEncryptor 的密鑰來源分開（不同的環境變數、不同的預設值）：加密跟雜湊索引是兩種
	// 不同用途的密鑰，其中一把外洩不該連帶讓另一把也不安全。
	private static final String DEV_ONLY_DEFAULT_SECRET = "dev-only-insecure-default-email-hash-key";

	private static final SecretKeySpec KEY = loadKey();

	private EmailLookupHasher() {
	}

	public static String hash(String email) {
		if (email == null) {
			return null;
		}
		try {
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(KEY);
			byte[] digest = mac.doFinal(email.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("email 雜湊失敗", e);
		}
	}

	private static SecretKeySpec loadKey() {
		String secret = System.getenv("EMAIL_HASH_KEY");
		if (secret == null || secret.isBlank()) {
			secret = DEV_ONLY_DEFAULT_SECRET;
		}
		try {
			byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
			return new SecretKeySpec(keyBytes, ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
