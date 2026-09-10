package com.example.jsppractice.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.jsppractice.dto.BookSummary;
import com.example.jsppractice.mapper.BookMapper;
import com.example.jsppractice.model.Book;
import com.example.jsppractice.model.Category;
import com.example.jsppractice.model.PageReq;
import com.example.jsppractice.model.PageRes;

@Service
public class BookServiceImpl implements BookService {

	private final BookMapper bookMapper;

	public BookServiceImpl(BookMapper bookMapper) {
		this.bookMapper = bookMapper;
	}

	@Override
	public List<BookSummary> findAll() {
		return bookMapper.findAll();
	}

	@Override
	public Book findById(Long id) {
		Book book = bookMapper.findById(id);
		if (book == null) {
			throw new NoSuchElementException("Book not found: " + id);
		}
		return book;
	}

	@Override
	public Book save(Book book) {
		if (book.getId() == null) {
			bookMapper.insert(book);
			return book;
		}
		bookMapper.update(book);
		return book;
	}

	@Override
	public void deleteById(Long id) {
		bookMapper.deleteById(id);
	}

	@Override
	@Transactional
	public void saveCategories(Long bookId, List<Long> categoryIds) {
		bookMapper.deleteCategoriesByBookId(bookId);
		if (categoryIds != null && !categoryIds.isEmpty()) {
			bookMapper.insertBookCategories(bookId, categoryIds);
		}
	}

	@Override
	public List<Category> findCategoriesById(Long bookId) {
		return bookMapper.findCategoriesById(bookId);
	}

	@Override
	public List<BookSummary> search(String keyWord) {
		return bookMapper.search(keyWord);
	}

	@Override
	public PageRes<BookSummary> search(String keyWord, PageReq pageReq) {
		long totalElements = bookMapper.countBySearch(keyWord);
		if (totalElements == 0) {
			return new PageRes<>(List.of(), pageReq.pageNumber(), pageReq.pageSize(), 0);
		}

		List<Long> ids = bookMapper.findIdsBySearch(keyWord, pageReq);
		if (ids.isEmpty()) {
			return new PageRes<>(List.of(), pageReq.pageNumber(), pageReq.pageSize(), totalElements);
		}

		List<BookSummary> content = bookMapper.findByIds(ids);
		return new PageRes<>(content, pageReq.pageNumber(), pageReq.pageSize(), totalElements);
	}
}
