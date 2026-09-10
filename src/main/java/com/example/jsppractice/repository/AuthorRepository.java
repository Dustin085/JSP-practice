package com.example.jsppractice.repository;

import java.util.List;
import java.util.Optional;

import com.example.jsppractice.model.Author;
import com.example.jsppractice.model.PageReq;

public interface AuthorRepository {

	List<Author> findAll();

	List<Author> findAll(PageReq pageReq);

	long count();

	Optional<Author> findById(Long id);

	Author insert(Author author);

	void update(Author author);

	void deleteById(Long id);

}
