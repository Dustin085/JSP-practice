package com.example.jsppractice.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.jsppractice.model.AuditLog;
import com.example.jsppractice.model.AuditLogCursor;
import com.example.jsppractice.model.CursorPage;
import com.example.jsppractice.service.AuditService;

@Controller
@RequestMapping("/audit-logs")
public class AuditLogController {

	private static final int PAGE_SIZE = 20;

	private final AuditService auditService;

	public AuditLogController(AuditService auditService) {
		this.auditService = auditService;
	}

	@GetMapping
	public String list(@RequestParam(required = false) Long after, @RequestParam(required = false) String afterAt,
			@RequestParam(required = false) Long before, @RequestParam(required = false) String beforeAt,
			Model model) {
		AuditLogCursor cursor = before != null ? toCursor(before, beforeAt) : toCursor(after, afterAt);
		CursorPage<AuditLog> page = before != null ? auditService.findPreviousPage(cursor, PAGE_SIZE)
				: auditService.findNextPage(cursor, PAGE_SIZE);

		List<AuditLog> content = page.content();
		model.addAttribute("auditLogs", content);
		model.addAttribute("hasNext", page.hasNext());
		model.addAttribute("hasPrev", page.hasPrev());
		if (!content.isEmpty()) {
			// content 依 audited_at、id 由大到小排序：第一筆最新（上一頁的 cursor）、最後一筆最舊（下一頁的 cursor）
			// cursor 的時間戳一定要原封不動傳回去（ISO-8601 字串，保留完整精度），不能轉成 epoch millis
			// 再轉回來——millis 只能精確到毫秒，捨去/進位任何一種都會讓 cursor 自己那筆資料在
			// 邊界比較時被誤判（自己包含自己，或漏掉緊鄰的下一筆），造成分頁重複或跳筆
			AuditLog first = content.get(0);
			AuditLog last = content.get(content.size() - 1);
			model.addAttribute("prevCursor", first.getId());
			model.addAttribute("prevCursorAt", first.getAuditedAt().toString());
			model.addAttribute("nextCursor", last.getId());
			model.addAttribute("nextCursorAt", last.getAuditedAt().toString());
		}
		return "audit-logs/list";
	}

	private AuditLogCursor toCursor(Long id, String auditedAt) {
		return id == null ? null : new AuditLogCursor(Instant.parse(auditedAt), id);
	}
}
