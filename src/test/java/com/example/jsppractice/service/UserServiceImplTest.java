package com.example.jsppractice.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.example.jsppractice.crypto.EmailLookupHasher;
import com.example.jsppractice.dto.UserRoleRow;
import com.example.jsppractice.exception.BaselineRoleCannotBeRevokedException;
import com.example.jsppractice.exception.LastRoleCannotBeRemovedException;
import com.example.jsppractice.exception.SelfAdminRevocationNotAllowedException;
import com.example.jsppractice.mapper.UserMapper;
import com.example.jsppractice.model.AuditLog;
import com.example.jsppractice.model.RoleType;
import com.example.jsppractice.model.User;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceImplTest {

	@Mock
	private UserMapper userMapper;

	@Mock
	private AuditService auditService;

	@InjectMocks
	private UserServiceImpl userService;

	@Test
	public void findAllAttachesRolesLookedUpInBatch() {
		User alex = User.builder().id(1L).email("alex@example.com").name("Alex").build();
		User brian = User.builder().id(2L).email("brian@example.com").name("Brian").build();
		when(userMapper.findAll()).thenReturn(List.of(alex, brian));
		when(userMapper.findRolesForUserIds(List.of(1L, 2L)))
				.thenReturn(List.of(new UserRoleRow(1L, RoleType.USER), new UserRoleRow(2L, RoleType.ADMIN),
						new UserRoleRow(2L, RoleType.USER)));

		List<User> users = userService.findAll();

		assertEquals(Set.of(RoleType.USER), users.get(0).getRoles());
		assertEquals(Set.of(RoleType.ADMIN, RoleType.USER), users.get(1).getRoles());
	}

	@Test
	public void findAllDoesNotQueryRolesWhenNoUsers() {
		when(userMapper.findAll()).thenReturn(List.of());

		List<User> users = userService.findAll();

		assertEquals(0, users.size());
		verify(userMapper, never()).findRolesForUserIds(any());
	}

	// findByEmail 不能直接拿明文去問 mapper——email 欄位在 DB 裡是密文，mapper 只認得
	// 盲索引雜湊，所以這裡驗證的是「有沒有先算雜湊、拿雜湊去查」，而不是拿明文去查
	@Test
	public void findByEmailQueriesMapperByHashNotPlainEmail() {
		String email = "admin@example.com";
		String expectedHash = EmailLookupHasher.hash(email);
		User admin = User.builder().id(1L).email(email).name("管理員").build();
		when(userMapper.findByEmailHash(expectedHash)).thenReturn(Optional.of(admin));
		when(userMapper.findRolesByUserId(1L)).thenReturn(Set.of());

		Optional<User> found = userService.findByEmail(email);

		assertEquals(admin, found.get());
		verify(userMapper).findByEmailHash(eq(expectedHash));
	}

	@Test
	public void findByEmailReturnsEmptyWhenHashNotFound() {
		when(userMapper.findByEmailHash(EmailLookupHasher.hash("nobody@example.com"))).thenReturn(Optional.empty());

		Optional<User> found = userService.findByEmail("nobody@example.com");

		assertFalse(found.isPresent());
	}

	private User admin(Long id) {
		return User.builder().id(id).email("admin@example.com").roles(Set.of(RoleType.ADMIN)).build();
	}

	@Test
	public void grantRoleInsertsRoleAndRecordsAuditLog() {
		when(userMapper.findByIds(List.of(5L))).thenReturn(List.of(User.builder().id(5L).build()));
		when(userMapper.findRolesByUserId(5L)).thenReturn(Set.of(RoleType.USER));

		userService.grantRole(5L, RoleType.PROCUREMENT, admin(1L));

		verify(userMapper).insertUserRoles(5L, List.of(RoleType.PROCUREMENT));
		verify(auditService).create(any(AuditLog.class));
	}

	@Test(expected = IllegalStateException.class)
	public void grantRoleThrowsWhenUserAlreadyHasRole() {
		when(userMapper.findByIds(List.of(5L))).thenReturn(List.of(User.builder().id(5L).build()));
		when(userMapper.findRolesByUserId(5L)).thenReturn(Set.of(RoleType.USER));

		userService.grantRole(5L, RoleType.USER, admin(1L));
	}

	@Test(expected = NoSuchElementException.class)
	public void grantRoleThrowsWhenUserDoesNotExist() {
		when(userMapper.findByIds(List.of(999L))).thenReturn(List.of());

		userService.grantRole(999L, RoleType.USER, admin(1L));
	}

	@Test
	public void revokeRoleDeletesRoleAndRecordsAuditLog() {
		when(userMapper.findByIds(List.of(5L))).thenReturn(List.of(User.builder().id(5L).build()));
		when(userMapper.findRolesByUserId(5L)).thenReturn(Set.of(RoleType.USER, RoleType.PROCUREMENT));

		userService.revokeRole(5L, RoleType.PROCUREMENT, admin(1L));

		verify(userMapper).deleteUserRole(5L, RoleType.PROCUREMENT);
		verify(auditService).create(any(AuditLog.class));
	}

	@Test(expected = IllegalStateException.class)
	public void revokeRoleThrowsWhenUserDoesNotHaveRole() {
		when(userMapper.findByIds(List.of(5L))).thenReturn(List.of(User.builder().id(5L).build()));
		when(userMapper.findRolesByUserId(5L)).thenReturn(Set.of(RoleType.USER));

		userService.revokeRole(5L, RoleType.PROCUREMENT, admin(1L));
	}

	@Test(expected = BaselineRoleCannotBeRevokedException.class)
	public void revokeRoleThrowsWhenRevokingUserRoleRegardlessOfHowManyRolesRemain() {
		// USER 這條規則不看目前角色數量、也不查資料庫就直接擋下來，所以這裡刻意不 stub
		// findByIds/findRolesByUserId——如果哪天實作改成先查再擋，這個測試也還是成立
		userService.revokeRole(5L, RoleType.USER, admin(1L));
	}

	@Test(expected = LastRoleCannotBeRemovedException.class)
	public void revokeRoleThrowsWhenItWouldLeaveUserWithNoRoles() {
		// 模擬資料本身不含 USER 的假設情境（正常流程一定會有 USER，這裡只是要單獨測到
		// 「拿掉最後一個角色」這條防呆本身，不要跟上面 USER 專屬的規則混在一起）
		when(userMapper.findByIds(List.of(5L))).thenReturn(List.of(User.builder().id(5L).build()));
		when(userMapper.findRolesByUserId(5L)).thenReturn(Set.of(RoleType.PROCUREMENT));

		userService.revokeRole(5L, RoleType.PROCUREMENT, admin(1L));
	}

	@Test(expected = SelfAdminRevocationNotAllowedException.class)
	public void revokeRoleThrowsWhenAdminTriesToRevokeOwnAdminRole() {
		when(userMapper.findByIds(List.of(1L))).thenReturn(List.of(User.builder().id(1L).build()));
		when(userMapper.findRolesByUserId(1L)).thenReturn(Set.of(RoleType.ADMIN, RoleType.USER));

		userService.revokeRole(1L, RoleType.ADMIN, admin(1L));
	}

	@Test
	public void revokeRoleAllowsAdminToRevokeAnotherUsersAdminRole() {
		when(userMapper.findByIds(List.of(2L))).thenReturn(List.of(User.builder().id(2L).build()));
		when(userMapper.findRolesByUserId(2L)).thenReturn(Set.of(RoleType.ADMIN, RoleType.USER));

		userService.revokeRole(2L, RoleType.ADMIN, admin(1L));

		verify(userMapper).deleteUserRole(2L, RoleType.ADMIN);
	}
}
