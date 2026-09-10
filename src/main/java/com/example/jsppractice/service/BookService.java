package com.example.jsppractice.service;

import java.util.List;

import com.example.jsppractice.dto.BookSummary;
import com.example.jsppractice.model.Book;
import com.example.jsppractice.model.Category;
import com.example.jsppractice.model.PageReq;
import com.example.jsppractice.model.PageRes;

public interface BookService {

	List<BookSummary> findAll();

	Book findById(Long id);

	Book save(Book book);

	void deleteById(Long id);

	void saveCategories(Long bookId, List<Long> categoryIds);

	List<Category> findCategoriesById(Long bookId);

	List<BookSummary> search(String keyWord);

	PageRes<BookSummary> search(String keyWord, PageReq pageReq);
}
