package com.example.jsppractice.mybatis;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.ibatis.type.JdbcType;
import org.junit.Test;

import com.example.jsppractice.crypto.AesEncryptor;

public class EncryptedStringTypeHandlerTest {

	private final EncryptedStringTypeHandler typeHandler = new EncryptedStringTypeHandler();

	@Test
	public void setNonNullParameterStoresEncryptedValue() throws Exception {
		PreparedStatement ps = mock(PreparedStatement.class);

		typeHandler.setNonNullParameter(ps, 1, "admin@example.com", JdbcType.VARCHAR);

		verify(ps).setString(eq(1), any(String.class));
		verify(ps, org.mockito.Mockito.never()).setString(anyInt(), eq("admin@example.com"));
	}

	@Test
	public void getNullableResultByColumnNameDecryptsStoredValue() throws Exception {
		ResultSet rs = mock(ResultSet.class);
		when(rs.getString("email")).thenReturn(AesEncryptor.encrypt("admin@example.com"));

		String result = typeHandler.getNullableResult(rs, "email");

		assertEquals("admin@example.com", result);
	}

	@Test
	public void getNullableResultByColumnIndexDecryptsStoredValue() throws Exception {
		ResultSet rs = mock(ResultSet.class);
		when(rs.getString(2)).thenReturn(AesEncryptor.encrypt("admin@example.com"));

		String result = typeHandler.getNullableResult(rs, 2);

		assertEquals("admin@example.com", result);
	}
}
