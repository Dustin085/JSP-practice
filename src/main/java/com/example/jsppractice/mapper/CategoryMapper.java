package com.example.jsppractice.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.jsppractice.model.Book;
import com.example.jsppractice.model.Category;
import com.example.jsppractice.model.PageReq;

public interface CategoryMapper {
	List<Category> findAll();

	List<Category> findAllPaged(@Param("pageReq") PageReq pageReq);

	long count();

	Category findById(Long id);

	List<Book> findBooksByCategoryId(Long id);

	void insert(Category category);

	void update(Category category);

	void deleteById(Long id);
}
