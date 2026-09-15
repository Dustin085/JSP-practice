package com.example.jsppractice.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class AesEncryptorTest {

	@Test
	public void decryptReversesEncrypt() {
		String plaintext = "admin@example.com";

		String ciphertext = AesEncryptor.encrypt(plaintext);

		assertEquals(plaintext, AesEncryptor.decrypt(ciphertext));
	}

	@Test
	public void encryptingSamePlaintextTwiceProducesDifferentCiphertext() {
		String plaintext = "admin@example.com";

		String first = AesEncryptor.encrypt(plaintext);
		String second = AesEncryptor.encrypt(plaintext);

		assertNotEquals("每次加密都要用新的隨機 IV，同樣明文不該產生同樣密文", first, second);
		assertEquals(plaintext, AesEncryptor.decrypt(first));
		assertEquals(plaintext, AesEncryptor.decrypt(second));
	}

	@Test
	public void encryptAndDecryptReturnNullForNullInput() {
		assertNull(AesEncryptor.encrypt(null));
		assertNull(AesEncryptor.decrypt(null));
	}

	@Test(expected = IllegalStateException.class)
	public void decryptThrowsWhenCiphertextIsTamperedWith() {
		String ciphertext = AesEncryptor.encrypt("admin@example.com");
		// 動第 4 個字元（落在 IV 的 base64 編碼範圍內，不會踩到字串尾端的 '=' padding），
		// GCM 的認證標籤驗證應該要抓到這已經不是原本那份密文
		char original = ciphertext.charAt(4);
		char replacement = original == 'A' ? 'B' : 'A';
		String tampered = ciphertext.substring(0, 4) + replacement + ciphertext.substring(5);

		AesEncryptor.decrypt(tampered);
	}
}
