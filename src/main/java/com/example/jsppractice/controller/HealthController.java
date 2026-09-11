package com.example.jsppractice.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

	private static final Logger log = LoggerFactory.getLogger(HealthController.class);

	private final JdbcTemplate jdbcTemplate;

	public HealthController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping("/health")
	public ResponseEntity<Map<String, Object>> health() {
		boolean dbUp = isDbUp();

		Map<String, Object> db = new LinkedHashMap<>();
		db.put("status", dbUp ? "UP" : "DOWN");

		Map<String, Object> components = new LinkedHashMap<>();
		components.put("db", db);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", dbUp ? "UP" : "DOWN");
		body.put("components", components);

		HttpStatus httpStatus = dbUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
		return ResponseEntity.status(httpStatus).body(body);
	}

	private boolean isDbUp() {
		try {
			jdbcTemplate.queryForObject("SELECT 1", Integer.class);
			return true;
		} catch (Exception e) {
			log.error("健康檢查失敗：資料庫連線異常", e);
			return false;
		}
	}
}
