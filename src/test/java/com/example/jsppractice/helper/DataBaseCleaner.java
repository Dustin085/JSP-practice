package com.example.jsppractice.helper;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DataBaseCleaner {
	private final JdbcTemplate jdbcTemplate;

	public void clean() {
		List<String> tables = jdbcTemplate.queryForList("SELECT table_name " + "FROM information_schema.tables "
				+ "WHERE table_schema = 'PUBLIC' AND table_type = 'BASE TABLE'", String.class);

		jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
		for (String table : tables) {
			jdbcTemplate.execute("TRUNCATE TABLE " + table);
		}
		jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
	}
}
