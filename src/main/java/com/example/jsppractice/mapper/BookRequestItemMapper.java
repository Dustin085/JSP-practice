package com.example.jsppractice.mapper;

import java.util.List;

import com.example.jsppractice.model.BookRequestItem;

public interface BookRequestItemMapper {

	BookRequestItem findById(Long id);

	List<BookRequestItem> findByBookRequestId(Long bookRequestId);

	void insert(BookRequestItem bookRequestItem);
}
