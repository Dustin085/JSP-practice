package com.example.jsppractice.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.jsppractice.exception.ProcurementAlreadyCompletedException;
import com.example.jsppractice.model.ProcurementStatus;
import com.example.jsppractice.model.User;
import com.example.jsppractice.security.CustomUserDetails;
import com.example.jsppractice.service.ProcurementService;

@Controller
@RequestMapping("/procurement")
public class ProcurementController {
	private final ProcurementService procurementService;

	public ProcurementController(ProcurementService procurementService) {
		this.procurementService = procurementService;
	}

	@GetMapping
	public String list(@RequestParam(required = false, defaultValue = "PENDING") String status, Model model) {
		ProcurementStatus filter = "ALL".equals(status) ? null : ProcurementStatus.valueOf(status);
		model.addAttribute("procurementItems", procurementService.findSummary(filter));
		model.addAttribute("statusFilter", status);
		return "procurement/list";
	}

	@PostMapping("/{id}/complete")
	public String complete(@PathVariable Long id, Authentication authentication,
			RedirectAttributes redirectAttributes) {
		User currentUser = CustomUserDetails.currentUser(authentication);
		try {
			procurementService.completeProcurement(id, currentUser);
			redirectAttributes.addFlashAttribute("flashMessage", "採購已完成，書籍已入庫");
		} catch (ProcurementAlreadyCompletedException e) {
			redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
		}
		return "redirect:/procurement";
	}
}
