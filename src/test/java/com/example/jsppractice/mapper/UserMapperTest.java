package com.example.jsppractice.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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
import com.example.jsppractice.crypto.AesEncryptor;
import com.example.jsppractice.crypto.EmailLookupHasher;
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

	// 繞過 UserMapper 直接塞資料，模擬「資料庫裡已經有這筆使用者」的情境，所以要自己先把 email
	// 加密、算好盲索引雜湊，存進去的格式才會跟真正透過 UserMapper.insert() 寫入的一致
	private Long insertUser(String email, String name, String passwordHash, RoleType role) {
		SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource).withTableName("users")
				.usingGeneratedKeyColumns("id");
		Map<String, Object> params = new HashMap<>();
		params.put("email", AesEncryptor.encrypt(email));
		params.put("email_lookup_hash", EmailLookupHasher.hash(email));
		params.put("name", name);
		params.put("password_hash", passwordHash);
		Long id = insert.executeAndReturnKey(params).longValue();
		jdbcTemplate.update("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", id, role.name());
		return id;
	}

	@Test
	public void findByEmailHashReturnsUserWithDecryptedEmail() {
		insertUser("alex@example.com", "Alex", "hashed-password", RoleType.USER);

		Optional<User> found = userMapper.findByEmailHash(EmailLookupHasher.hash("alex@example.com"));

		assertTrue(found.isPresent());
		assertEquals("alex@example.com", found.get().getEmail());
		assertEquals("Alex", found.get().getName());
		assertEquals("hashed-password", found.get().getPasswordHash());
	}

	@Test
	public void findByEmailHashReturnsEmptyWhenNotFound() {
		Optional<User> found = userMapper.findByEmailHash(EmailLookupHasher.hash("nobody@example.com"));

		assertFalse(found.isPresent());
	}

	@Test
	public void insertAssignsGeneratedIdAndStoresEmailEncrypted() {
		User user = User.builder().email("new@example.com").name("New User").passwordHash("hashed-password")
				.roles(Set.of(RoleType.ADMIN)).build();

		userMapper.insert(user, EmailLookupHasher.hash(user.getEmail()));
		userMapper.insertUserRoles(user.getId(), user.getRoles());

		String rawEmailColumn = jdbcTemplate.queryForObject("SELECT email FROM users WHERE id = ?", String.class,
				user.getId());
		assertNotEquals("email 欄位存的應該是密文，不是明文", "new@example.com", rawEmailColumn);

		User found = userMapper.findByEmailHash(EmailLookupHasher.hash("new@example.com")).get();
		assertEquals(user.getId(), found.getId());
		assertEquals("new@example.com", found.getEmail());
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

		User found = userMapper.findByEmailHash(EmailLookupHasher.hash("update@example.com")).get();
		assertEquals("New Name", found.getName());
		assertEquals("new-hash", found.getPasswordHash());
		assertEquals(Set.of(RoleType.ADMIN), userMapper.findRolesByUserId(id));
	}

	@Test
	public void findAllReturnsEveryUserOrderedByIdWithDecryptedEmail() {
		Long firstId = insertUser("alex@example.com", "Alex", "hashed-password", RoleType.USER);
		Long secondId = insertUser("brian@example.com", "Brian", "hashed-password", RoleType.ADMIN);

		List<User> users = userMapper.findAll();

		assertEquals(2, users.size());
		assertEquals(firstId, users.get(0).getId());
		assertEquals("alex@example.com", users.get(0).getEmail());
		assertEquals(secondId, users.get(1).getId());
		assertEquals("brian@example.com", users.get(1).getEmail());
	}

	@Test
	public void findByIdsReturnsOnlyRequestedUsersWithDecryptedEmail() {
		Long alexId = insertUser("alex@example.com", "Alex", "hashed-password", RoleType.USER);
		insertUser("brian@example.com", "Brian", "hashed-password", RoleType.ADMIN);
		Long carolId = insertUser("carol@example.com", "Carol", "hashed-password", RoleType.PROCUREMENT);

		List<User> users = userMapper.findByIds(List.of(alexId, carolId));

		assertEquals(2, users.size());
		assertEquals(Set.of("alex@example.com", "carol@example.com"),
				users.stream().map(User::getEmail).collect(java.util.stream.Collectors.toSet()));
	}

	@Test
	public void deleteUserRoleRemovesOnlyThatRole() {
		Long id = insertUser("alex@example.com", "Alex", "hashed-password", RoleType.USER);
		userMapper.insertUserRoles(id, List.of(RoleType.ADMIN));

		userMapper.deleteUserRole(id, RoleType.ADMIN);

		assertEquals(Set.of(RoleType.USER), userMapper.findRolesByUserId(id));
	}

	@Test
	public void findRolesForUserIdsGroupsRolesByUser() {
		Long alexId = insertUser("alex@example.com", "Alex", "hashed-password", RoleType.USER);
		userMapper.insertUserRoles(alexId, List.of(RoleType.ADMIN));
		Long brianId = insertUser("brian@example.com", "Brian", "hashed-password", RoleType.PROCUREMENT);

		List<com.example.jsppractice.dto.UserRoleRow> rows = userMapper.findRolesForUserIds(List.of(alexId, brianId));

		Set<RoleType> alexRoles = rows.stream().filter(row -> row.userId().equals(alexId))
				.map(com.example.jsppractice.dto.UserRoleRow::role).collect(java.util.stream.Collectors.toSet());
		Set<RoleType> brianRoles = rows.stream().filter(row -> row.userId().equals(brianId))
				.map(com.example.jsppractice.dto.UserRoleRow::role).collect(java.util.stream.Collectors.toSet());
		assertEquals(Set.of(RoleType.USER, RoleType.ADMIN), alexRoles);
		assertEquals(Set.of(RoleType.PROCUREMENT), brianRoles);
	}
}
