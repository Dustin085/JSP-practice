package com.example.jsppractice.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.jsppractice.mapper.UserMapper;
import com.example.jsppractice.model.User;
import com.example.jsppractice.security.CustomUserDetails;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	private final UserMapper userMapper;

	public UserDetailsServiceImpl(UserMapper userMapper) {
		this.userMapper = userMapper;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User user = userMapper.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("帳號不存在：" + email));
		return new CustomUserDetails(user);
	}
}
