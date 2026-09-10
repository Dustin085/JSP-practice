package com.example.jsppractice.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class BookValidationTest {

	private static ValidatorFactory factory;
	private static Validator validator;

	@BeforeClass
	public static void setUpValidator() {
		factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@AfterClass
	public static void closeFactory() {
		factory.close();
	}

	@Test
	public void blankTitleIsRejected() {
		Book book = new Book(null, "", "9780134685991", 1L, 2018);

		Set<ConstraintViolation<Book>> violations = validator.validate(book);

		assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
	}

	@Test
	public void invalidIsbnIsRejected() {
		Book book = new Book(null, "Effective Java", "not-an-isbn", 1L, 2018);

		Set<ConstraintViolation<Book>> violations = validator.validate(book);

		assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("isbn")));
	}

	@Test
	public void publishedYearOutOfRangeIsRejected() {
		Book book = new Book(null, "Effective Java", "", 1L, 3000);

		Set<ConstraintViolation<Book>> violations = validator.validate(book);

		assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("publishedYear")));
	}

	@Test
	public void validBookHasNoViolations() {
		Book book = new Book(null, "Effective Java", "9780134685991", 1L, 2018);

		Set<ConstraintViolation<Book>> violations = validator.validate(book);

		assertEquals(0, violations.size());
	}
}
