package com.example.jsppractice.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.example.jsppractice.mapper.CategoryMapper;
import com.example.jsppractice.model.Category;
import com.example.jsppractice.model.PageReq;
import com.example.jsppractice.model.PageRes;

@Service
public class CategoryServiceImpl implements CategoryService {

	private final CategoryMapper categoryMapper;

	public CategoryServiceImpl(CategoryMapper categoryMapper) {
		this.categoryMapper = categoryMapper;
	}

	@Override
	public List<Category> findAll() {
		return categoryMapper.findAll();
	}

	@Override
	public PageRes<Category> findAll(PageReq pageReq) {
		long totalElements = categoryMapper.count();
		List<Category> content = totalElements == 0 ? List.of() : categoryMapper.findAllPaged(pageReq);
		return new PageRes<>(content, pageReq.pageNumber(), pageReq.pageSize(), totalElements);
	}

	@Override
	public Category findById(Long id) {
		Category category = categoryMapper.findById(id);
		if (category == null) {
			throw new NoSuchElementException("Category not found: " + id);
		}
		return category;
	}

	@Override
	public Category save(Category category) {
		if (category.getId() == null) {
			categoryMapper.insert(category);
			return category;
		}
		categoryMapper.update(category);
		return category;
	}

	@Override
	public void deleteById(Long id) {
		categoryMapper.deleteById(id);
	}
}
