package com.example.jsppractice.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// AES-256/GCM：對稱加密 + 內建完整性驗證（跟 CBC 不一樣，密文被竄改過 decrypt 會直接丟例外，
// 不會默默解出一堆亂碼）。GCM 要求每次加密的 IV（12 bytes）不能重複，這裡每次呼叫都重新隨機產生，
// 跟密文存在一起（IV 不是秘密，只要求不重複），格式是 Base64(iv || 密文+16 bytes 認證標籤)。
public final class AesEncryptor {

	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final int GCM_TAG_LENGTH_BITS = 128;
	private static final int GCM_IV_LENGTH_BYTES = 12;

	// 金鑰不能寫死在程式碼裡（跟密碼一樣的道理，這裡用 BCryptPasswordEncoder 雜湊密碼是同一個原則）。
	// 正式環境一定要設 AES_SECRET_KEY 環境變數；沒設就退回這個明顯標記為「僅供本機開發」的預設值，
	// 純粹是為了本機起 Tomcat 不用額外設環境變數就能跑，換成生產環境用同一把公開在原始碼裡的金鑰
	// 等於沒加密，部署時務必覆蓋。
	private static final String DEV_ONLY_DEFAULT_SECRET = "dev-only-insecure-default-aes-key";

	private static final SecretKey KEY = loadKey();

	private AesEncryptor() {
	}

	public static String encrypt(String plaintext) {
		if (plaintext == null) {
			return null;
		}
		try {
			byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
			SecureRandom.getInstanceStrong().nextBytes(iv);

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, KEY, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
			byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

			ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
			buffer.put(iv).put(ciphertext);
			return Base64.getEncoder().encodeToString(buffer.array());
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("AES 加密失敗", e);
		}
	}

	public static String decrypt(String encoded) {
		if (encoded == null) {
			return null;
		}
		try {
			byte[] decoded = Base64.getDecoder().decode(encoded);
			ByteBuffer buffer = ByteBuffer.wrap(decoded);
			byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
			buffer.get(iv);
			byte[] ciphertext = new byte[buffer.remaining()];
			buffer.get(ciphertext);

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, KEY, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
			return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("AES 解密失敗：金鑰不對，或密文被竄改過", e);
		}
	}

	// 允許環境變數放任意長度的密語（passphrase），不用逼人手動產生/貼上一把格式正確的 256-bit 金鑰——
	// SHA-256 雜湊後輸出剛好 32 bytes，直接當 AES-256 金鑰用。這只是把「任意輸入」正規化成「正確長度」，
	// 不會替低熵的密語增加熵，金鑰安全性還是取決於環境變數裡那個密語本身夠不夠隨機。
	private static SecretKey loadKey() {
		String secret = System.getenv("AES_SECRET_KEY");
		if (secret == null || secret.isBlank()) {
			secret = DEV_ONLY_DEFAULT_SECRET;
		}
		try {
			byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
			return new SecretKeySpec(keyBytes, "AES");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
