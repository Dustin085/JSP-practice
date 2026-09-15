package com.example.jsppractice.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class EmailMaskerTest {

	@Test
	public void masksMiddleOfLocalPartForLongEmail() {
		assertEquals("a***n@example.com", EmailMasker.mask("admin@example.com"));
	}

	@Test
	public void masksShortLocalPartKeepingOnlyFirstChar() {
		assertEquals("a***@example.com", EmailMasker.mask("ab@example.com"));
	}

	@Test
	public void masksSingleCharLocalPart() {
		assertEquals("a***@example.com", EmailMasker.mask("a@example.com"));
	}

	@Test
	public void masksEntireStringWhenNoAtSign() {
		assertEquals("***", EmailMasker.mask("not-an-email"));
	}

	@Test
	public void returnsNullForNullInput() {
		assertNull(EmailMasker.mask(null));
	}
}
