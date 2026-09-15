package com.example.jsppractice.mapper;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.apache.ibatis.annotations.Param;

import com.example.jsppractice.model.RoleType;
import com.example.jsppractice.model.User;

public interface UserMapper {
	Optional<User> findByEmail(String email);

	void insert(User user);

	void update(User user);

	Set<RoleType> findRolesByUserId(Long userId);

	void insertUserRoles(@Param("userId") Long userId, @Param("roles") Collection<RoleType> roles);

	void deleteRolesByUserId(Long userId);
}
