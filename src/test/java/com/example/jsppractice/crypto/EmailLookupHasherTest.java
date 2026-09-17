package com.example.jsppractice.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class EmailLookupHasherTest {

	@Test
	public void sameEmailAlwaysProducesSameHash() {
		String first = EmailLookupHasher.hash("admin@example.com");
		String second = EmailLookupHasher.hash("admin@example.com");

		assertEquals("確定性雜湊：同樣輸入要每次都一樣，才能拿來查找", first, second);
	}

	@Test
	public void differentEmailsProduceDifferentHashes() {
		assertNotEquals(EmailLookupHasher.hash("admin@example.com"), EmailLookupHasher.hash("user@example.com"));
	}

	@Test
	public void hashIsSixtyFourHexCharacters() {
		String hash = EmailLookupHasher.hash("admin@example.com");

		assertEquals(64, hash.length());
		assertEquals(hash, hash.toLowerCase());
	}

	@Test
	public void returnsNullForNullInput() {
		assertNull(EmailLookupHasher.hash(null));
	}
}
