package com.example.jsppractice.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import com.example.jsppractice.dto.ProcurementSummary;
import com.example.jsppractice.model.ProcurementItem;
import com.example.jsppractice.model.ProcurementStatus;

public interface ProcurementItemMapper {

	Optional<ProcurementItem> findById(Long id);

	Optional<ProcurementItem> findByIdForUpdate(Long id);

	List<ProcurementItem> findByStatus(@Param("status") ProcurementStatus status);

	List<ProcurementSummary> findSummary(@Param("status") ProcurementStatus status);

	void insert(ProcurementItem procurementItem);

	void update(ProcurementItem procurementItem);
}
