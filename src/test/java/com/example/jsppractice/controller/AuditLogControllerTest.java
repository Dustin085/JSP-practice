package com.example.jsppractice.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.jsppractice.dto.AuditLogSummary;
import com.example.jsppractice.model.AuditActionType;
import com.example.jsppractice.model.AuditEntityType;
import com.example.jsppractice.model.AuditLogCursor;
import com.example.jsppractice.model.CursorPage;
import com.example.jsppractice.service.AuditService;

@RunWith(MockitoJUnitRunner.class)
public class AuditLogControllerTest {

	@Mock
	private AuditService auditService;

	@InjectMocks
	private AuditLogController auditLogController;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(auditLogController).build();
	}

	// 真實情境常見：H2 儲存時間戳時會四捨五入到奈秒精度，讀回來的 Instant 常常不是整毫秒。
	// 這裡故意用非整毫秒的時間，模擬先前用 epoch millis 來回傳遞時 toEpochMilli() 只會捨去、
	// 把自己這筆資料的邊界比較搞錯的情況（上一頁自己撈回自己、漏掉最新那筆）。
	private AuditLogSummary logWithSubMillisecondPrecision(long id, Instant auditedAt) {
		return AuditLogSummary.builder().id(id).userEmail("auditor@example.com").auditedAt(auditedAt)
				.action(AuditActionType.APPROVE).entityType(AuditEntityType.BOOK_REQUEST).entityId(1L)
				.detail(Collections.emptyMap()).build();
	}

	private Map<String, Object> modelOf(MvcResult result) {
		return result.getModelAndView().getModel();
	}

	@Test
	public void nextCursorAtRoundTripsExactInstantWithoutLosingSubMillisecondPrecision() throws Exception {
		Instant preciseInstant = Instant.parse("2026-09-14T11:36:41.434567891Z");
		AuditLogSummary newest = logWithSubMillisecondPrecision(24L, Instant.parse("2026-09-14T11:36:41.500000000Z"));
		AuditLogSummary last = logWithSubMillisecondPrecision(5L, preciseInstant);
		when(auditService.findNextPage(eq(null), eq(20)))
				.thenReturn(new CursorPage<>(List.of(newest, last), true, false));

		MvcResult firstPage = mockMvc.perform(get("/audit-logs")).andReturn();
		String nextCursorAt = (String) modelOf(firstPage).get("nextCursorAt");
		assertEquals(preciseInstant, Instant.parse(nextCursorAt));

		// 模擬瀏覽器帶著這個字串點下一頁：cursor 還原出來的 Instant 必須跟原本完全一致
		when(auditService.findNextPage(any(AuditLogCursor.class), eq(20)))
				.thenReturn(new CursorPage<>(List.of(), false, true));
		mockMvc.perform(get("/audit-logs").param("after", "5").param("afterAt", nextCursorAt));
		ArgumentCaptor<AuditLogCursor> cursorCaptor = ArgumentCaptor.forClass(AuditLogCursor.class);
		verify(auditService, org.mockito.Mockito.times(2)).findNextPage(cursorCaptor.capture(), eq(20));
		AuditLogCursor secondCallCursor = cursorCaptor.getAllValues().get(1);
		assertEquals(preciseInstant, secondCallCursor.auditedAt());
		assertEquals(Long.valueOf(5L), secondCallCursor.id());
	}

	@Test
	public void prevCursorAtRoundTripsExactInstantWithoutLosingSubMillisecondPrecision() throws Exception {
		Instant preciseInstant = Instant.parse("2026-09-14T11:36:41.434567891Z");
		AuditLogSummary first = logWithSubMillisecondPrecision(3L, preciseInstant);
		AuditLogSummary older = logWithSubMillisecondPrecision(2L, Instant.parse("2026-09-14T11:36:41.000000000Z"));
		when(auditService.findNextPage(any(), eq(20))).thenReturn(new CursorPage<>(List.of(first, older), false, true));

		MvcResult page = mockMvc.perform(get("/audit-logs")).andReturn();
		String prevCursorAt = (String) modelOf(page).get("prevCursorAt");
		assertEquals(preciseInstant, Instant.parse(prevCursorAt));

		// 模擬瀏覽器帶著這個字串點上一頁：findPreviousPage 拿到的 cursor 一定要是原始精確值，
		// 不可以是被 epoch millis 捨去過、比自己還「舊」的版本（否則會把自己也撈回來）
		when(auditService.findPreviousPage(any(AuditLogCursor.class), eq(20)))
				.thenReturn(new CursorPage<>(List.of(), true, false));
		mockMvc.perform(get("/audit-logs").param("before", "3").param("beforeAt", prevCursorAt));
		ArgumentCaptor<AuditLogCursor> cursorCaptor = ArgumentCaptor.forClass(AuditLogCursor.class);
		verify(auditService).findPreviousPage(cursorCaptor.capture(), eq(20));
		assertEquals(preciseInstant, cursorCaptor.getValue().auditedAt());
		assertEquals(Long.valueOf(3L), cursorCaptor.getValue().id());
	}
}
