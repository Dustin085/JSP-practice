package com.example.jsppractice.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import com.example.jsppractice.dto.BookRequestSummary;
import com.example.jsppractice.model.BookRequest;
import com.example.jsppractice.model.BookRequestStatus;

public interface BookRequestMapper {
	List<BookRequest> findAll();

	Optional<BookRequest> findById(Long id);

	Optional<BookRequest> findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

	List<BookRequest> findByStatus(@Param("status") BookRequestStatus status);

	List<BookRequestSummary> findSummary(@Param("status") BookRequestStatus status);

	int approve(@Param("bookRequestId") Long bookRequestId, @Param("approverId") Long approverId,
			@Param("approvedAt") LocalDateTime approvedAt);

	int reject(@Param("bookRequestId") Long bookRequestId, @Param("approverId") Long approverId,
			@Param("approvedAt") LocalDateTime approvedAt);

	void insert(BookRequest bookRequest);
}
