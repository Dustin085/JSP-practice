package com.example.jsppractice.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.jsppractice.model.AuditLog;
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
	public String list(@RequestParam(required = false) Long after, @RequestParam(required = false) Long before,
			Model model) {
		CursorPage<AuditLog> page = before != null ? auditService.findPreviousPage(before, PAGE_SIZE)
				: auditService.findNextPage(after, PAGE_SIZE);

		List<AuditLog> content = page.content();
		model.addAttribute("auditLogs", content);
		model.addAttribute("hasNext", page.hasNext());
		model.addAttribute("hasPrev", page.hasPrev());
		if (!content.isEmpty()) {
			// content 依 id 由大到小排序：第一筆最新（上一頁的 cursor）、最後一筆最舊（下一頁的 cursor）
			model.addAttribute("prevCursor", content.get(0).getId());
			model.addAttribute("nextCursor", content.get(content.size() - 1).getId());
		}
		return "audit-logs/list";
	}
}
