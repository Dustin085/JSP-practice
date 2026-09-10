package com.example.jsppractice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
	private Long id;

	private String email;

	private String name;

	private String passwordHash;

	private RoleType role;
}
