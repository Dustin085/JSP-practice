package com.example.jsppractice.controller;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.jsppractice.dto.ReconciliationSummary;
import com.example.jsppractice.model.Book;
import com.example.jsppractice.model.Reconciliation;
import com.example.jsppractice.model.User;
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

	@PostMapping("/items/{id}/resolve")
	public String resolve(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
		User currentUser = (User) session.getAttribute("currentUser");
		reconciliationService.resolve(id, currentUser);
		redirectAttributes.addFlashAttribute("flashMessage", "已標記為已處理");
		return "redirect:/reconciliations";
	}

	@PostMapping("/items/{id}/write-off")
	public String writeOff(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
		User currentUser = (User) session.getAttribute("currentUser");
		reconciliationService.writeOff(id, currentUser);
		redirectAttributes.addFlashAttribute("flashMessage", "已沖銷，下次對帳不會再回報這筆");
		return "redirect:/reconciliations";
	}

	@PostMapping("/items/{id}/backfill-procurement-item")
	public String backfillProcurementItem(@PathVariable Long id, HttpSession session,
			RedirectAttributes redirectAttributes) {
		User currentUser = (User) session.getAttribute("currentUser");
		reconciliationService.backfillProcurementItem(id, currentUser);
		redirectAttributes.addFlashAttribute("flashMessage", "已補建對應的採購項目");
		return "redirect:/reconciliations";
	}

	@GetMapping("/items/{id}/relink")
	public String relinkForm(@PathVariable Long id, Model model) {
		List<Book> candidates = reconciliationService.findRelinkCandidates(id);
		model.addAttribute("reconciliationItemId", id);
		model.addAttribute("candidates", candidates);
		return "reconciliations/relink";
	}

	@PostMapping("/items/{id}/relink")
	public String relink(@PathVariable Long id, @RequestParam Long bookId, HttpSession session,
			RedirectAttributes redirectAttributes) {
		User currentUser = (User) session.getAttribute("currentUser");
		reconciliationService.relink(id, bookId, currentUser);
		redirectAttributes.addFlashAttribute("flashMessage", "已重新連結書籍");
		return "redirect:/reconciliations";
	}

	@PostMapping("/items/{id}/revert-to-pending")
	public String revertToPending(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
		User currentUser = (User) session.getAttribute("currentUser");
		reconciliationService.revertToPending(id, currentUser);
		redirectAttributes.addFlashAttribute("flashMessage", "已退回待處理，可以到採購清單重新完成採購");
		return "redirect:/reconciliations";
	}
}
