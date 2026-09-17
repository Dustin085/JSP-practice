package com.example.jsppractice.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.jsppractice.dto.BookSummary;
import com.example.jsppractice.model.Book;
import com.example.jsppractice.model.Category;
import com.example.jsppractice.model.PageReq;

public interface BookMapper {
	List<BookSummary> findAll();

	Book findById(Long id);

	void insert(Book book);

	int update(Book book);

	void deleteById(Long id);

	List<Book> findByAuthorId(Long id);

	List<Category> findCategoriesById(Long id);

	void deleteCategoriesByBookId(Long id);

	void insertBookCategories(@Param("bookId") Long bookId, @Param("categoryIds") List<Long> categoryIds);

	List<BookSummary> search(@Param("keyWord") String keyWord);

	List<Long> findIdsBySearch(@Param("keyWord") String keyWord, @Param("pageReq") PageReq pageReq);

	long countBySearch(@Param("keyWord") String keyWord);

	List<BookSummary> findByIds(@Param("ids") List<Long> ids);

	List<Book> findRelinkCandidates(@Param("title") String title, @Param("isbn") String isbn);
}
