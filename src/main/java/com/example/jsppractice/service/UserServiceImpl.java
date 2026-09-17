package com.example.jsppractice.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.jsppractice.crypto.EmailLookupHasher;
import com.example.jsppractice.dto.UserRoleRow;
import com.example.jsppractice.exception.BaselineRoleCannotBeRevokedException;
import com.example.jsppractice.exception.LastRoleCannotBeRemovedException;
import com.example.jsppractice.exception.SelfAdminRevocationNotAllowedException;
import com.example.jsppractice.mapper.UserMapper;
import com.example.jsppractice.model.AuditActionType;
import com.example.jsppractice.model.AuditEntityType;
import com.example.jsppractice.model.AuditLog;
import com.example.jsppractice.model.RoleType;
import com.example.jsppractice.model.User;

@Service
public class UserServiceImpl implements UserService {
	private UserMapper userMapper;
	private final AuditService auditService;

	public UserServiceImpl(UserMapper userMapper, AuditService auditService) {
		this.userMapper = userMapper;
		this.auditService = auditService;
	}

	@Override
	@Transactional
	public User save(User user) {
		if (user.getId() == null) {
			userMapper.insert(user, EmailLookupHasher.hash(user.getEmail()));
		} else {
			// email 目前沒有編輯功能，update() 也不會動 email 欄位，不需要重算/更新 email_lookup_hash
			userMapper.update(user);
			userMapper.deleteRolesByUserId(user.getId());
		}
		if (user.getRoles() != null && !user.getRoles().isEmpty()) {
			userMapper.insertUserRoles(user.getId(), user.getRoles());
		}
		return user;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<User> findByEmail(String email) {
		return userMapper.findByEmailHash(EmailLookupHasher.hash(email)).map(user -> {
			user.setRoles(userMapper.findRolesByUserId(user.getId()));
			return user;
		});
	}

	@Override
	@Transactional(readOnly = true)
	public List<User> findAll() {
		List<User> users = userMapper.findAll();
		if (users.isEmpty()) {
			return users;
		}
		// deferred join：先查完使用者清單，才批次查這一批使用者的角色，跟 AuditServiceImpl
		// 批次查 email 是同一種手法，避免對每一列使用者各查一次 findRolesByUserId 造成 N+1
		List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
		Map<Long, Set<RoleType>> rolesByUserId = userMapper.findRolesForUserIds(userIds).stream()
				.collect(Collectors.groupingBy(UserRoleRow::userId, Collectors.mapping(UserRoleRow::role,
						Collectors.toSet())));
		users.forEach(user -> user.setRoles(rolesByUserId.getOrDefault(user.getId(), Set.of())));
		return users;
	}

	@Override
	@Transactional
	public void grantRole(Long userId, RoleType role, User currentUser) {
		Set<RoleType> currentRoles = findRolesOrThrow(userId);
		if (currentRoles.contains(role)) {
			throw new IllegalStateException("這個帳號已經有這個角色了：userId=" + userId + ", role=" + role);
		}
		userMapper.insertUserRoles(userId, List.of(role));
		auditService.create(buildRoleChangeAuditLog(currentUser, AuditActionType.GRANT_ROLE, userId, role));
	}

	@Override
	@Transactional
	public void revokeRole(Long userId, RoleType role, User currentUser) {
		// 防呆零：USER 是每個帳號都必須有的 baseline，本來就不是「使用者手動掛上去」的角色
		// （註冊、DataSeeder 都是自動加的），不管是誰、不管這個帳號現在還有沒有其他角色，
		// 一律不能移除——比下面「不能拿掉最後一個角色」更直接，不用等到只剩它一個才擋。
		if (role == RoleType.USER) {
			throw new BaselineRoleCannotBeRevokedException("USER 是每個帳號都必須有的基礎角色，不能被移除");
		}
		Set<RoleType> currentRoles = findRolesOrThrow(userId);
		if (!currentRoles.contains(role)) {
			throw new IllegalStateException("這個帳號本來就沒有這個角色：userId=" + userId + ", role=" + role);
		}
		// 防呆一：不管是誰動的手，都不能讓一個帳號被拿掉最後一個角色——上面 USER 已經不能被移除，
		// 理論上這裡不會再被觸發到，但角色種類以後可能變動，留著當一層不依賴 USER 特例的保險。
		if (currentRoles.size() <= 1) {
			throw new LastRoleCannotBeRemovedException("每個帳號至少要保留一個角色，不能移除僅剩的最後一個角色");
		}
		// 防呆二：不能移除自己的 ADMIN，就算自己還有其他角色、就算還有別的 ADMIN 存在也一樣——
		// 不用另外查「是不是最後一個 ADMIN」，規則越單純越不會有 race condition，
		// 要拔某個 ADMIN 的權限一定要由別的 ADMIN 動手，不能自己單方面解除自己的管理權限。
		if (userId.equals(currentUser.getId()) && role == RoleType.ADMIN) {
			throw new SelfAdminRevocationNotAllowedException("不能移除自己的 ADMIN 角色，請改由其他管理員操作");
		}
		userMapper.deleteUserRole(userId, role);
		auditService.create(buildRoleChangeAuditLog(currentUser, AuditActionType.REVOKE_ROLE, userId, role));
	}

	private Set<RoleType> findRolesOrThrow(Long userId) {
		if (userMapper.findByIds(List.of(userId)).isEmpty()) {
			throw new NoSuchElementException("找不到使用者：id=" + userId);
		}
		return userMapper.findRolesByUserId(userId);
	}

	private AuditLog buildRoleChangeAuditLog(User currentUser, AuditActionType action, Long targetUserId,
			RoleType role) {
		Map<String, Object> detail = new HashMap<>();
		detail.put("role", role);
		return AuditLog.builder().userId(currentUser.getId()).auditedAt(Instant.now()).action(action)
				.entityType(AuditEntityType.USER).entityId(targetUserId).detail(detail).build();
	}

}
