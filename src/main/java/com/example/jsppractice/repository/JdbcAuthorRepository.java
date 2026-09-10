package com.example.jsppractice.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import com.example.jsppractice.model.Author;
import com.example.jsppractice.model.PageReq;

@Repository
public class JdbcAuthorRepository implements AuthorRepository {

	private static final RowMapper<Author> ROW_MAPPER = (rs, rowNum) -> new Author(rs.getLong("id"),
			rs.getString("name"));

	private final JdbcTemplate jdbcTemplate;
	private final SimpleJdbcInsert insertAuthor;

	public JdbcAuthorRepository(JdbcTemplate jdbcTemplate, DataSource dataSource) {
		this.jdbcTemplate = jdbcTemplate;
		this.insertAuthor = new SimpleJdbcInsert(dataSource).withTableName("authors").usingGeneratedKeyColumns("id");
	}

	@Override
	public List<Author> findAll() {
		return jdbcTemplate.query("SELECT * FROM authors ORDER BY id", ROW_MAPPER);
	}

	@Override
	public List<Author> findAll(PageReq pageReq) {
		return jdbcTemplate.query("SELECT * FROM authors ORDER BY id LIMIT ? OFFSET ?", ROW_MAPPER,
				pageReq.pageSize(), pageReq.offset());
	}

	@Override
	public long count() {
		Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM authors", Long.class);
		return count == null ? 0 : count;
	}

	@Override
	public Optional<Author> findById(Long id) {
		return jdbcTemplate.query("SELECT * FROM authors WHERE id = ?", ROW_MAPPER, id).stream().findFirst();
	}

	@Override
	public Author insert(Author author) {
		Map<String, Object> params = new HashMap<>();
		params.put("name", author.getName());
		Number id = insertAuthor.executeAndReturnKey(params);
		author.setId(id.longValue());
		return author;
	}

	@Override
	public void update(Author author) {
		jdbcTemplate.update("UPDATE authors SET name = ? WHERE id = ?", author.getName(), author.getId());
	}

	@Override
	public void deleteById(Long id) {
		jdbcTemplate.update("DELETE FROM authors WHERE id = ?", id);
	}

}
