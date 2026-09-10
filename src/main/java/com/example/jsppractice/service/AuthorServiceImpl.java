package com.example.jsppractice.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.example.jsppractice.model.Author;
import com.example.jsppractice.model.PageReq;
import com.example.jsppractice.model.PageRes;
import com.example.jsppractice.repository.AuthorRepository;

@Service
public class AuthorServiceImpl implements AuthorService {
	private final AuthorRepository authorRepository;

	public AuthorServiceImpl(AuthorRepository authorRepository) {
		this.authorRepository = authorRepository;
	}

	@Override
	public List<Author> findAll() {
		return authorRepository.findAll();
	}

	@Override
	public PageRes<Author> findAll(PageReq pageReq) {
		long totalElements = authorRepository.count();
		List<Author> content = totalElements == 0 ? List.of() : authorRepository.findAll(pageReq);
		return new PageRes<>(content, pageReq.pageNumber(), pageReq.pageSize(), totalElements);
	}

	@Override
	public Author findById(Long id) {
		return authorRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Author not found: " + id));
	}

	@Override
	public Author save(Author author) {
		if (author.getId() == null) {
			return authorRepository.insert(author);
		}
		authorRepository.update(author);
		return author;
	}

	@Override
	public void deleteById(Long id) {
		authorRepository.deleteById(id);
	}

}
