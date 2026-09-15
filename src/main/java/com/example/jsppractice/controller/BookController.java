package com.example.jsppractice.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.jsppractice.dto.BookSummary;
import com.example.jsppractice.model.Book;
import com.example.jsppractice.model.Category;
import com.example.jsppractice.model.PageReq;
import com.example.jsppractice.model.PageRes;
import com.example.jsppractice.service.AuthorService;
import com.example.jsppractice.service.BookExporter;
import com.example.jsppractice.service.BookService;
import com.example.jsppractice.service.CategoryService;

@Controller
@RequestMapping("/books")
public class BookController {

	private final BookService bookService;
	private final AuthorService authorService;
	private final CategoryService categoryService;
	private final BookExporter bookExporter;

	private static final int PAGE_SIZE = 10;

	public BookController(BookService bookService, AuthorService authorService, CategoryService categoryService,
			BookExporter bookExporter) {
		this.bookService = bookService;
		this.authorService = authorService;
		this.categoryService = categoryService;
		this.bookExporter = bookExporter;
	}

	@GetMapping
	public String list(@RequestParam(required = false) String keyword,
			@RequestParam(required = false, defaultValue = "0") int page, Model model) {
		PageRes<BookSummary> books = bookService.search(keyword, new PageReq(page, PAGE_SIZE));
		model.addAttribute("books", books.content());
		model.addAttribute("pageNumber", books.pageNumber());
		model.addAttribute("totalPages", books.totalPages());
		model.addAttribute("keyword", keyword);
		return "books/list";
	}

	@GetMapping("/new")
	public String newForm(Model model) {
		model.addAttribute("book", new Book());
		model.addAttribute("authors", authorService.findAll());
		model.addAttribute("categories", categoryService.findAll());
		model.addAttribute("selectedCategoryIds", Collections.emptyList());
		return "books/form";
	}

	@PostMapping
	public String create(@Valid Book book, BindingResult bindingResult,
			@RequestParam(required = false) List<Long> categoryIds, Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("authors", authorService.findAll());
			model.addAttribute("categories", categoryService.findAll());
			model.addAttribute("selectedCategoryIds", categoryIds == null ? Collections.emptyList() : categoryIds);
			return "books/form";
		}
		Book saved = bookService.save(book);
		bookService.saveCategories(saved.getId(), categoryIds);
		redirectAttributes.addFlashAttribute("flashMessage", "書籍新增成功");
		return "redirect:/books";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		model.addAttribute("book", bookService.findById(id));
		model.addAttribute("authors", authorService.findAll());
		model.addAttribute("categories", categoryService.findAll());
		List<Long> selectedCategoryIds = bookService.findCategoriesById(id).stream().map(Category::getId)
				.collect(Collectors.toList());
		model.addAttribute("selectedCategoryIds", selectedCategoryIds);
		return "books/form";
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id, @Valid Book book, BindingResult bindingResult,
			@RequestParam(required = false) List<Long> categoryIds, Model model,
			RedirectAttributes redirectAttributes) {
		book.setId(id);
		if (bindingResult.hasErrors()) {
			model.addAttribute("authors", authorService.findAll());
			model.addAttribute("categories", categoryService.findAll());
			model.addAttribute("selectedCategoryIds", categoryIds == null ? Collections.emptyList() : categoryIds);
			return "books/form";
		}
		try {
			bookService.save(book);
			bookService.saveCategories(id, categoryIds);
			redirectAttributes.addFlashAttribute("flashMessage", "書籍更新成功");
		} catch (OptimisticLockingFailureException e) {
			redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
		}
		return "redirect:/books";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		bookService.deleteById(id);
		redirectAttributes.addFlashAttribute("flashMessage", "書籍已刪除");
		return "redirect:/books";
	}

	@GetMapping("/export")
	public void export(HttpServletResponse response, @RequestParam(required = false) String keyword)
			throws IOException {
		List<BookSummary> bookSummaries = bookService.search(keyword);
		String fileName = URLEncoder.encode("書籍列表.xlsx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
		response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		try (Workbook workbook = bookExporter.exportToExcel(bookSummaries)) {
			workbook.write(response.getOutputStream());
		}
	}
}
