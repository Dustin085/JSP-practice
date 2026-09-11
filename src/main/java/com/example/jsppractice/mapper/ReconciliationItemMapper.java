package com.example.jsppractice.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import com.example.jsppractice.model.ReconciliationItem;
import com.example.jsppractice.model.ReconciliationItemStatus;

public interface ReconciliationItemMapper {
	Optional<ReconciliationItem> findById(Long id);

	void insert(ReconciliationItem reconciliationItem);

	void updateStatus(@Param("id") Long id, @Param("status") ReconciliationItemStatus status);
}
