package com.example.jsppractice.controller;

import javax.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.jsppractice.model.Category;
import com.example.jsppractice.model.PageReq;
import com.example.jsppractice.model.PageRes;
import com.example.jsppractice.service.CategoryService;

@Controller
@RequestMapping("/categories")
public class CategoryController {

	private static final int PAGE_SIZE = 10;

	private final CategoryService categoryService;

	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}

	@GetMapping
	public String list(@RequestParam(required = false, defaultValue = "0") int page, Model model) {
		PageRes<Category> categories = categoryService.findAll(new PageReq(page, PAGE_SIZE));
		model.addAttribute("categories", categories.content());
		model.addAttribute("pageNumber", categories.pageNumber());
		model.addAttribute("totalPages", categories.totalPages());
		return "categories/list";
	}

	@GetMapping("/new")
	public String newForm(Model model) {
		model.addAttribute("category", new Category());
		return "categories/form";
	}

	@PostMapping
	public String create(@Valid Category category, BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return "categories/form";
		}
		categoryService.save(category);
		redirectAttributes.addFlashAttribute("flashMessage", "分類新增成功");
		return "redirect:/categories";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		model.addAttribute("category", categoryService.findById(id));
		return "categories/form";
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id, @Valid Category category, BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		category.setId(id);
		if (bindingResult.hasErrors()) {
			return "categories/form";
		}
		categoryService.save(category);
		redirectAttributes.addFlashAttribute("flashMessage", "分類更新成功");
		return "redirect:/categories";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		categoryService.deleteById(id);
		redirectAttributes.addFlashAttribute("flashMessage", "分類已刪除");
		return "redirect:/categories";
	}
}
