package com.example.jsppractice.mapper.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 讓 MyBatis 在 Map<String, Object> 屬性 (Java 端) 與 JSON 欄位 (DB 端) 之間自動轉換,
 * 只在 mapper XML 用 typeHandler 屬性指定給 detail 欄位使用。
 */
@MappedTypes(Map.class)
public class JsonMapTypeHandler extends BaseTypeHandler<Map<String, Object>> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Override
	public void setNonNullParameter(PreparedStatement ps, int columnIndex, Map<String, Object> parameter,
			JdbcType jdbcType) throws SQLException {
		try {
			// H2 的 JSON 欄位透過 setString() 綁定時不會把字串內容當 JSON 結構解析,
			// 只會把整個字串包成一個 JSON string scalar (等於整包被多包一層引號)。
			// 依 H2 文件,要用 UTF-8 bytes (setBytes) 才會被當成 JSON 內容解析。
			byte[] json = OBJECT_MAPPER.writeValueAsBytes(parameter);
			ps.setBytes(columnIndex, json);
		} catch (Exception e) {
			throw new SQLException("無法將 detail 序列化為 JSON", e);
		}
	}

	@Override
	public Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return toMap(rs.getString(columnName));
	}

	@Override
	public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return toMap(rs.getString(columnIndex));
	}

	@Override
	public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return toMap(cs.getString(columnIndex));
	}

	private Map<String, Object> toMap(String json) throws SQLException {
		if (json == null) {
			return null;
		}
		try {
			return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception e) {
			throw new SQLException("無法解析 detail JSON", e);
		}
	}
}
