package com.example.jsppractice.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.example.jsppractice.crypto.AesEncryptor;

// 掛在 UserMapper.xml 的 email 欄位（INSERT/UPDATE 參數用 #{...,typeHandler=...}，SELECT 用
// resultMap 指定），讓加解密完全在 JDBC 存取這一層做掉——User model、service、controller
// 拿到的、傳進來的永遠是明文 email，完全不用知道 DB 裡實際存的是密文，就跟 JPA 的
// AttributeConverter 是同一個概念，只是 MyBatis 這邊叫 TypeHandler。
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
			throws SQLException {
		ps.setString(i, AesEncryptor.encrypt(parameter));
	}

	@Override
	public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return AesEncryptor.decrypt(rs.getString(columnName));
	}

	@Override
	public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return AesEncryptor.decrypt(rs.getString(columnIndex));
	}

	@Override
	public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return AesEncryptor.decrypt(cs.getString(columnIndex));
	}
}
