package com.example.jsppractice.service;

import java.util.List;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.example.jsppractice.dto.BookSummary;

@Component
public class BookExporter {
	public Workbook exportToExcel(List<BookSummary> bookSummaries) {
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet();
		workbook.setSheetName(workbook.getSheetIndex(sheet), "Books");
		List<BookCellNameMapping> bookCellNameMappings = List.of(new BookCellNameMapping("ID", 0, BookSummary::getId),
				new BookCellNameMapping("書名", 1, BookSummary::getTitle),
				new BookCellNameMapping("ISBN", 2, BookSummary::getIsbn),
				new BookCellNameMapping("出版年", 3, BookSummary::getPublishedYear),
				new BookCellNameMapping("作者", 4, BookSummary::getAuthorName),
				new BookCellNameMapping("分類", 5, BookSummary::getCategoriesString));
		// 標頭
		Row headerRow = sheet.createRow(0);
		for (BookCellNameMapping bookCellNameMapping : bookCellNameMappings) {
			Cell cell = headerRow.createCell(bookCellNameMapping.cellIndex);
			cell.setCellValue(bookCellNameMapping.fieldName);
		}
		// 印出 BookSummary
		int DATAROW_START = 1;
		for (int rIdx = DATAROW_START; rIdx < bookSummaries.size() + DATAROW_START; rIdx++) {
			Row row = sheet.createRow(rIdx);
			BookSummary bookSummary = bookSummaries.get(rIdx - DATAROW_START);
			for (BookCellNameMapping bookCellNameMapping : bookCellNameMappings) {
				Cell cell = row.createCell(bookCellNameMapping.cellIndex);
				Object value = bookCellNameMapping.fn.apply(bookSummary);
				if (value instanceof String s) {
					cell.setCellValue(s);
				} else if (value instanceof Number n) { // Long、Integer、Double 都繼承 Number
					cell.setCellValue(n.doubleValue()); // POI 數字統一接收 double
				}
			}
		}

		return workbook;
	}

	record BookCellNameMapping(String fieldName, int cellIndex, Function<BookSummary, ?> fn) {
	}
}
