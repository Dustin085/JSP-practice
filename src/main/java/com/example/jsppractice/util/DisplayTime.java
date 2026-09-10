package com.example.jsppractice.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DisplayTime {

	private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private DisplayTime() {
	}

	public static String format(Instant instant) {
		return instant == null ? "" : FORMATTER.format(instant.atZone(TAIPEI));
	}
}
