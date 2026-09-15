package com.example.jsppractice.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.ibatis.annotations.Param;

import com.example.jsppractice.model.RoleType;
import com.example.jsppractice.model.User;

public interface UserMapper {
	// 拿盲索引（HMAC-SHA256(email)）查，不是明文 email——email 欄位本身是密文，沒辦法用等於比對。
	Optional<User> findByEmailHash(String emailHash);

	List<User> findAll();

	void insert(@Param("user") User user, @Param("emailHash") String emailHash);

	void update(User user);

	Set<RoleType> findRolesByUserId(Long userId);

	void insertUserRoles(@Param("userId") Long userId, @Param("roles") Collection<RoleType> roles);

	void deleteRolesByUserId(Long userId);
}
