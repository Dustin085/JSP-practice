package com.example.jsppractice.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.jsppractice.dto.ReconciliationSummary;
import com.example.jsppractice.model.Reconciliation;
import com.example.jsppractice.service.ReconciliationService;

@Controller
@RequestMapping("/reconciliations")
public class ReconciliationController {

	private final ReconciliationService reconciliationService;

	public ReconciliationController(ReconciliationService reconciliationService) {
		this.reconciliationService = reconciliationService;
	}

	@GetMapping
	public String list(Model model) {
		List<ReconciliationSummary> reconciliations = reconciliationService.findSummary();
		model.addAttribute("reconciliations", reconciliations);
		return "reconciliations/list";
	}

	@PostMapping("/book-request-items")
	public String reconcileBookRequestItems(RedirectAttributes redirectAttributes) {
		Reconciliation result = reconciliationService.reconcileBookRequestItems();
		redirectAttributes.addFlashAttribute("flashMessage", "申請/採購對帳完成，結果：" + result.getStatus());
		return "redirect:/reconciliations";
	}

	@PostMapping("/procurement-items")
	public String reconcileProcurementItems(RedirectAttributes redirectAttributes) {
		Reconciliation result = reconciliationService.reconcileProcurementItems();
		redirectAttributes.addFlashAttribute("flashMessage", "採購/書籍對帳完成，結果：" + result.getStatus());
		return "redirect:/reconciliations";
	}
}
