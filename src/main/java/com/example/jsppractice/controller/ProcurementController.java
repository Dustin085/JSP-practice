package com.example.jsppractice.controller;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.jsppractice.exception.ProcurementAlreadyCompletedException;
import com.example.jsppractice.model.ProcurementStatus;
import com.example.jsppractice.model.User;
import com.example.jsppractice.service.ProcurementService;

@Controller
@RequestMapping("/procurement")
public class ProcurementController {
	private final ProcurementService procurementService;

	public ProcurementController(ProcurementService procurementService) {
		this.procurementService = procurementService;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("procurementItems", procurementService.findSummary(ProcurementStatus.PENDING));
		return "procurement/list";
	}

	@PostMapping("/{id}/complete")
	public String complete(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
		User currentUser = (User) session.getAttribute("currentUser");
		try {
			procurementService.completeProcurement(id, currentUser);
			redirectAttributes.addFlashAttribute("flashMessage", "採購已完成，書籍已入庫");
		} catch (ProcurementAlreadyCompletedException e) {
			redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
		}
		return "redirect:/procurement";
	}
}
