package com.example.jsppractice.service;

import java.util.List;

import com.example.jsppractice.model.Category;
import com.example.jsppractice.model.PageReq;
import com.example.jsppractice.model.PageRes;

public interface CategoryService {
	List<Category> findAll();

	PageRes<Category> findAll(PageReq pageReq);

	Category findById(Long id);

	Category save(Category category);

	void deleteById(Long id);
}
