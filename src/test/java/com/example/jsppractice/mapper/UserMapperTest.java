package com.example.jsppractice.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.example.jsppractice.config.RootConfig;
import com.example.jsppractice.helper.DataBaseCleaner;
import com.example.jsppractice.model.RoleType;
import com.example.jsppractice.model.User;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RootConfig.class)
public class UserMapperTest {

	@Autowired
	private UserMapper userMapper;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private DataBaseCleaner dataBaseCleaner;

	@Before
	public void setUp() {
		dataBaseCleaner = new DataBaseCleaner(jdbcTemplate);
	}

	@After
	public void tearDown() {
		dataBaseCleaner.clean();
	}

	private Long insertUser(String email, String name, String passwordHash, RoleType role) {
		SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource).withTableName("users")
				.usingGeneratedKeyColumns("id");
		Map<String, Object> params = new HashMap<>();
		params.put("email", email);
		params.put("name", name);
		params.put("password_hash", passwordHash);
		params.put("role", role.name());
		return insert.executeAndReturnKey(params).longValue();
	}

	@Test
	public void findByEmailReturnsUserWhenExists() {
		insertUser("alex@example.com", "Alex", "hashed-password", RoleType.USER);

		Optional<User> found = userMapper.findByEmail("alex@example.com");

		assertTrue(found.isPresent());
		assertEquals("Alex", found.get().getName());
		assertEquals("hashed-password", found.get().getPasswordHash());
		assertEquals(RoleType.USER, found.get().getRole());
	}

	@Test
	public void findByEmailReturnsEmptyWhenNotFound() {
		Optional<User> found = userMapper.findByEmail("nobody@example.com");

		assertFalse(found.isPresent());
	}

	@Test
	public void insertAssignsGeneratedId() {
		User user = User.builder().email("new@example.com").name("New User").passwordHash("hashed-password")
				.role(RoleType.ADMIN).build();

		userMapper.insert(user);

		User found = userMapper.findByEmail("new@example.com").get();
		assertEquals(user.getId(), found.getId());
		assertEquals(RoleType.ADMIN, found.getRole());
	}

	@Test
	public void updateChangesExistingRow() {
		Long id = insertUser("update@example.com", "Old Name", "old-hash", RoleType.USER);
		User user = User.builder().id(id).email("update@example.com").name("New Name").passwordHash("new-hash")
				.role(RoleType.ADMIN).build();

		userMapper.update(user);

		User found = userMapper.findByEmail("update@example.com").get();
		assertEquals("New Name", found.getName());
		assertEquals("new-hash", found.getPasswordHash());
		assertEquals(RoleType.ADMIN, found.getRole());
	}
}
