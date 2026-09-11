package com.example.jsppractice.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(MockitoJUnitRunner.class)
public class HealthControllerTest {

	@Mock
	private JdbcTemplate jdbcTemplate;

	@InjectMocks
	private HealthController healthController;

	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(healthController).build();
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> dbStatusOf(MvcResult result) throws Exception {
		Map<String, Object> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
		return (Map<String, Object>) body.get("components");
	}

	@Test
	public void returnsUpWhenDbRespondsToQuery() throws Exception {
		when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);

		MvcResult result = mockMvc.perform(get("/health")).andExpect(status().isOk()).andReturn();

		@SuppressWarnings("unchecked")
		Map<String, Object> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
		assertEquals("UP", body.get("status"));
		@SuppressWarnings("unchecked")
		Map<String, Object> db = (Map<String, Object>) dbStatusOf(result).get("db");
		assertEquals("UP", db.get("status"));
	}

	@Test
	public void returnsDownWithServiceUnavailableWhenDbQueryFails() throws Exception {
		when(jdbcTemplate.queryForObject(eq("SELECT 1"), any(Class.class)))
				.thenThrow(new DataAccessResourceFailureException("connection refused"));

		MvcResult result = mockMvc.perform(get("/health")).andExpect(status().isServiceUnavailable()).andReturn();

		@SuppressWarnings("unchecked")
		Map<String, Object> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
		assertEquals("DOWN", body.get("status"));
		@SuppressWarnings("unchecked")
		Map<String, Object> db = (Map<String, Object>) dbStatusOf(result).get("db");
		assertEquals("DOWN", db.get("status"));
	}
}
