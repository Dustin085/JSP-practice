package com.example.jsppractice.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.jsppractice.dto.DeliveryFileImportOutcome;
import com.example.jsppractice.exception.SftpOperationException;
import com.example.jsppractice.service.DeliveryImportScheduler;

@Controller
@RequestMapping("/deliveries")
public class DeliveryImportController {

	private final DeliveryImportScheduler deliveryImportScheduler;

	public DeliveryImportController(DeliveryImportScheduler deliveryImportScheduler) {
		this.deliveryImportScheduler = deliveryImportScheduler;
	}

	@GetMapping
	public String show() {
		return "deliveries/show";
	}

	// 跟排程（每天 01:00）共用同一個方法，這裡是「需要立即重跑」時的手動入口，
	// 跟 ReconciliationController 的對帳手動觸發是同一個道理
	@PostMapping("/run")
	public String run(RedirectAttributes redirectAttributes) {
		try {
			List<DeliveryFileImportOutcome> outcomes = deliveryImportScheduler.importPendingDeliveries();
			redirectAttributes.addFlashAttribute("outcomes", outcomes);
			redirectAttributes.addFlashAttribute("flashMessage", "已處理 " + outcomes.size() + " 個檔案");
		} catch (SftpOperationException e) {
			redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
		}
		return "redirect:/deliveries";
	}
}
