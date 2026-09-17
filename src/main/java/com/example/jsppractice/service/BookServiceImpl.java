package com.example.jsppractice.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.dao.OptimisticLockingFailureException;
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
		int updated = bookMapper.update(book);
		if (updated == 0) {
			throw new OptimisticLockingFailureException("這本書已經被其他人修改過，請重新整理再試一次：id=" + book.getId());
		}
		// UPDATE 裡實際遞增 version 的是資料庫（SET version = version + 1），這裡只是讓呼叫端
		// 拿到的這個物件也同步反映最新版本號，避免緊接著又拿同一個物件送第二次更新時，
		// 帶著已經過期的舊 version 白白被擋下來。
		book.setVersion(book.getVersion() + 1);
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
	@Transactional(readOnly = true)
	public PageRes<BookSummary> search(String keyWord, PageReq pageReq) {
		long totalElements = bookMapper.countBySearch(keyWord);
		if (totalElements == 0) {
			return new PageRes<>(List.of(), pageReq.pageNumber(), pageReq.pageSize(), 0);
		}

		// 先找出 ids 可以達到 Deferred Join 的效果，只搜尋 id 達到索引覆蓋避免 OFFSET 造成大量回表
		List<Long> ids = bookMapper.findIdsBySearch(keyWord, pageReq);
		if (ids.isEmpty()) {
			return new PageRes<>(List.of(), pageReq.pageNumber(), pageReq.pageSize(), totalElements);
		}

		List<BookSummary> content = bookMapper.findByIds(ids);
		return new PageRes<>(content, pageReq.pageNumber(), pageReq.pageSize(), totalElements);
	}
}
