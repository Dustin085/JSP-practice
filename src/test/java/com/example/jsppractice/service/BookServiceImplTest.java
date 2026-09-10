package com.example.jsppractice.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.example.jsppractice.mapper.BookMapper;
import com.example.jsppractice.model.Book;

@RunWith(MockitoJUnitRunner.class)
public class BookServiceImplTest {
	@Mock
	private BookMapper bookMapper;

	@InjectMocks
	private BookServiceImpl bookService;

	@Test
	public void saveWithNullIdCallsInsert() {
		Book book = new Book(null, "Effective Java", "978-0134685991", 1L, 2018);
		bookService.save(book);
		verify(bookMapper).insert(book);
		verify(bookMapper, never()).update(any());
	}

	@Test
	public void saveWithExsitingIdCallsUpdate() {
		Book book = new Book(5L, "Effective Java", "978-0134685991", 1L, 2018);
		bookService.save(book);
		verify(bookMapper).update(book);
		verify(bookMapper, never()).insert(any());
	}

	@Test
	public void findByIdReturnsBookWhenFound() {
		Book book = new Book(1L, "Effective Java", "978-0134685991", 1L, 2018);
		when(bookMapper.findById(1L)).thenReturn(book);

		assertEquals(book.getId(), bookService.findById(1L).getId());
	}

	@Test(expected = NoSuchElementException.class)
	public void findByIdThrowsWhenNotFound() {
		when(bookMapper.findById(2L)).thenReturn(null);

		bookService.findById(2L);
	}
}
