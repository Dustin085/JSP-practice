package com.example.jsppractice.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class DisplayJsonTest {

	@Test
	public void prettyPrintsNestedMapAsIndentedJson() {
		Map<String, Object> status = new LinkedHashMap<>();
		status.put("old", "PENDING");
		status.put("new", "APPROVED");
		Map<String, Object> detail = new LinkedHashMap<>();
		detail.put("status", status);

		String result = DisplayJson.prettyPrint(detail);

		assertTrue("應該要有換行才叫排版過，不是原本 Map.toString() 那種單行", result.contains("\n"));
		assertTrue(result.contains("\"old\" : \"PENDING\""));
		assertTrue(result.contains("\"new\" : \"APPROVED\""));
	}

	@Test
	public void returnsEmptyStringForNull() {
		assertEquals("", DisplayJson.prettyPrint(null));
	}
}
