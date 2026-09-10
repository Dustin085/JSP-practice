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

import com.example.jsppractice.model.Author;
import com.example.jsppractice.model.PageReq;
import com.example.jsppractice.model.PageRes;
import com.example.jsppractice.service.AuthorService;

@Controller
@RequestMapping("/authors")
public class AuthorController {

	private static final int PAGE_SIZE = 10;

	private final AuthorService authorService;

	public AuthorController(AuthorService authorService) {
		this.authorService = authorService;
	}

	@GetMapping
	public String list(@RequestParam(required = false, defaultValue = "0") int page, Model model) {
		PageRes<Author> authors = authorService.findAll(new PageReq(page, PAGE_SIZE));
		model.addAttribute("authors", authors.content());
		model.addAttribute("pageNumber", authors.pageNumber());
		model.addAttribute("totalPages", authors.totalPages());
		return "authors/list";
	}

	@GetMapping("/new")
	public String newForm(Model model) {
		model.addAttribute("author", new Author());
		return "authors/form";
	}

	@PostMapping
	public String create(@Valid Author author, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return "authors/form";
		}
		authorService.save(author);
		redirectAttributes.addFlashAttribute("flashMessage", "作者新增成功");
		return "redirect:/authors";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		model.addAttribute("author", authorService.findById(id));
		return "authors/form";
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id, @Valid Author author, BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		author.setId(id);
		if (bindingResult.hasErrors()) {
			return "authors/form";
		}
		authorService.save(author);
		redirectAttributes.addFlashAttribute("flashMessage", "作者更新成功");
		return "redirect:/authors";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		authorService.deleteById(id);
		redirectAttributes.addFlashAttribute("flashMessage", "作者已刪除");
		return "redirect:/authors";
	}
}
