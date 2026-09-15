package com.example.jsppractice.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
		Long id = insert.executeAndReturnKey(params).longValue();
		jdbcTemplate.update("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", id, role.name());
		return id;
	}

	@Test
	public void findByEmailReturnsUserWhenExists() {
		insertUser("alex@example.com", "Alex", "hashed-password", RoleType.USER);

		Optional<User> found = userMapper.findByEmail("alex@example.com");

		assertTrue(found.isPresent());
		assertEquals("Alex", found.get().getName());
		assertEquals("hashed-password", found.get().getPasswordHash());
	}

	@Test
	public void findByEmailReturnsEmptyWhenNotFound() {
		Optional<User> found = userMapper.findByEmail("nobody@example.com");

		assertFalse(found.isPresent());
	}

	@Test
	public void insertAssignsGeneratedId() {
		User user = User.builder().email("new@example.com").name("New User").passwordHash("hashed-password")
				.roles(Set.of(RoleType.ADMIN)).build();

		userMapper.insert(user);
		userMapper.insertUserRoles(user.getId(), user.getRoles());

		User found = userMapper.findByEmail("new@example.com").get();
		assertEquals(user.getId(), found.getId());
		assertEquals(Set.of(RoleType.ADMIN), userMapper.findRolesByUserId(found.getId()));
	}

	@Test
	public void updateChangesExistingRow() {
		Long id = insertUser("update@example.com", "Old Name", "old-hash", RoleType.USER);
		User user = User.builder().id(id).email("update@example.com").name("New Name").passwordHash("new-hash")
				.roles(Set.of(RoleType.ADMIN)).build();

		userMapper.update(user);
		userMapper.deleteRolesByUserId(id);
		userMapper.insertUserRoles(id, user.getRoles());

		User found = userMapper.findByEmail("update@example.com").get();
		assertEquals("New Name", found.getName());
		assertEquals("new-hash", found.getPasswordHash());
		assertEquals(Set.of(RoleType.ADMIN), userMapper.findRolesByUserId(id));
	}

	@Test
	public void findAllReturnsEveryUserOrderedById() {
		Long firstId = insertUser("alex@example.com", "Alex", "hashed-password", RoleType.USER);
		Long secondId = insertUser("brian@example.com", "Brian", "hashed-password", RoleType.ADMIN);

		List<User> users = userMapper.findAll();

		assertEquals(2, users.size());
		assertEquals(firstId, users.get(0).getId());
		assertEquals(secondId, users.get(1).getId());
	}
}
