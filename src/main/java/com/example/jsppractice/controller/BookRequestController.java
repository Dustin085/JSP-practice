package com.example.jsppractice.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.jsppractice.exception.BookRequestAlreadyProcessedException;
import com.example.jsppractice.exception.SelfReviewNotAllowedException;
import com.example.jsppractice.model.BookRequestStatus;
import com.example.jsppractice.model.User;
import com.example.jsppractice.service.AuthorService;
import com.example.jsppractice.service.BookRequestService;
import com.example.jsppractice.service.BookRequestService.BookRequestBookInfo;

@Controller
@RequestMapping("/requests")
public class BookRequestController {
	private final BookRequestService bookRequestService;
	private final AuthorService authorService;

	public BookRequestController(BookRequestService bookRequestService, AuthorService authorService) {
		this.bookRequestService = bookRequestService;
		this.authorService = authorService;
	}

	@GetMapping
	public String list(@RequestParam(required = false) BookRequestStatus status, Model model) {
		model.addAttribute("bookRequests", bookRequestService.findSummary(status));
		model.addAttribute("status", status);
		return "requests/list";
	}

	@GetMapping("/new")
	public String newForm(Model model) {
		model.addAttribute("authors", authorService.findAll());
		// 表單渲染當下產生一次性的 idempotency key，寫死在 hidden input 裡，
		// 不管使用者點幾次送出按鈕，帶出去的都是同一個值。
		model.addAttribute("idempotencyKey", UUID.randomUUID().toString());
		return "requests/form";
	}

	@PostMapping
	public String submit(@RequestParam List<String> title, @RequestParam List<String> isbn,
			@RequestParam List<String> authorId, @RequestParam List<String> publishedYear,
			@RequestParam List<String> estimatedPrice, @RequestParam String idempotencyKey, HttpSession session,
			RedirectAttributes redirectAttributes) {
		User currentUser = (User) session.getAttribute("currentUser");

		List<BookRequestBookInfo> bookInfos = new ArrayList<>();
		for (int i = 0; i < title.size(); i++) {
			if (isBlank(title.get(i))) {
				continue;
			}
			bookInfos.add(new BookRequestBookInfo(title.get(i), blankToNull(get(isbn, i)), parseLong(get(authorId, i)),
					parseInteger(get(publishedYear, i)), parseBigDecimal(get(estimatedPrice, i))));
		}

		if (bookInfos.isEmpty()) {
			redirectAttributes.addFlashAttribute("flashMessage", "至少需要填寫一本書的書名");
			return "redirect:/requests/new";
		}

		bookRequestService.submit(currentUser, bookInfos, idempotencyKey);
		redirectAttributes.addFlashAttribute("flashMessage", "申請已送出");
		return "redirect:/requests";
	}

	@PostMapping("/{id}/approve")
	public String approve(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
		User currentUser = (User) session.getAttribute("currentUser");
		try {
			bookRequestService.approve(id, currentUser);
			redirectAttributes.addFlashAttribute("flashMessage", "申請已核准");
		} catch (SelfReviewNotAllowedException | BookRequestAlreadyProcessedException e) {
			redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
		}
		return "redirect:/requests";
	}

	@PostMapping("/{id}/reject")
	public String reject(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
		User currentUser = (User) session.getAttribute("currentUser");
		try {
			bookRequestService.reject(id, currentUser);
			redirectAttributes.addFlashAttribute("flashMessage", "申請已拒絕");
		} catch (SelfReviewNotAllowedException | BookRequestAlreadyProcessedException e) {
			redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
		}
		return "redirect:/requests";
	}

	private String get(List<String> values, int index) {
		return index < values.size() ? values.get(index) : null;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private String blankToNull(String value) {
		return isBlank(value) ? null : value;
	}

	private Long parseLong(String value) {
		return isBlank(value) ? null : Long.valueOf(value);
	}

	private Integer parseInteger(String value) {
		return isBlank(value) ? null : Integer.valueOf(value);
	}

	private BigDecimal parseBigDecimal(String value) {
		return isBlank(value) ? null : new BigDecimal(value);
	}
}
