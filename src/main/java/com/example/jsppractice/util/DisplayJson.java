package com.example.jsppractice.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class DisplayJson {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private DisplayJson() {
	}

	// 給畫面顯示用，不是給程式解析用，所以失敗時退回 toString() 而不是丟例外——
	// 一個顯示不出好看格式的 detail 欄位，不該讓整個稽核紀錄列表頁掛掉。
	public static String prettyPrint(Object value) {
		if (value == null) {
			return "";
		}
		try {
			return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
		} catch (JsonProcessingException e) {
			return value.toString();
		}
	}
}
