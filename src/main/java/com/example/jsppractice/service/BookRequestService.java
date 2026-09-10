package com.example.jsppractice.service;

import java.math.BigDecimal;
import java.util.List;

import com.example.jsppractice.dto.BookRequestSummary;
import com.example.jsppractice.model.BookRequest;
import com.example.jsppractice.model.BookRequestItem;
import com.example.jsppractice.model.BookRequestStatus;
import com.example.jsppractice.model.User;

public interface BookRequestService {
	BookRequest submit(User requestUser, List<BookRequestBookInfo> bookInfos, String idempotencyKey);

	BookRequest approve(Long requestId, User currentUser);

	BookRequest reject(Long requestId, User currentUser);

	List<BookRequest> findAll();

	List<BookRequest> findByStatus(BookRequestStatus status);

	List<BookRequestItem> findItemsByRequestId(Long requestId);

	List<BookRequestSummary> findSummary(BookRequestStatus status);

	record BookRequestBookInfo(String title, String isbn, Long authorId, Integer publishedYear,
			BigDecimal estimatedPrice) {
	}
}
