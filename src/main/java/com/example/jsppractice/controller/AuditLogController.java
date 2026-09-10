package com.example.jsppractice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.jsppractice.model.AuditLog;
import com.example.jsppractice.model.PageReq;
import com.example.jsppractice.model.PageRes;
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
	public String list(@RequestParam(required = false, defaultValue = "0") int page, Model model) {
		PageRes<AuditLog> auditLogs = auditService.findAll(new PageReq(page, PAGE_SIZE));
		model.addAttribute("auditLogs", auditLogs.content());
		model.addAttribute("pageNumber", auditLogs.pageNumber());
		model.addAttribute("totalPages", auditLogs.totalPages());
		return "audit-logs/list";
	}
}
