package com.example.jsppractice.service;

import java.util.List;

import com.example.jsppractice.model.Author;
import com.example.jsppractice.model.PageReq;
import com.example.jsppractice.model.PageRes;

public interface AuthorService {
	List<Author> findAll();

	PageRes<Author> findAll(PageReq pageReq);

	Author findById(Long id);

	Author save(Author author);

	void deleteById(Long id);
}
