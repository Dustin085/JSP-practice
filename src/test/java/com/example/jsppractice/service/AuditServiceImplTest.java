package com.example.jsppractice.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.example.jsppractice.dto.AuditLogSummary;
import com.example.jsppractice.mapper.AuditLogMapper;
import com.example.jsppractice.mapper.UserMapper;
import com.example.jsppractice.model.AuditActionType;
import com.example.jsppractice.model.AuditEntityType;
import com.example.jsppractice.model.AuditLog;
import com.example.jsppractice.model.CursorPage;
import com.example.jsppractice.model.User;

@RunWith(MockitoJUnitRunner.class)
public class AuditServiceImplTest {

	@Mock
	private AuditLogMapper auditLogMapper;

	@Mock
	private UserMapper userMapper;

	@InjectMocks
	private AuditServiceImpl auditService;

	private AuditLog logByUser(long id, Long userId) {
		return AuditLog.builder().id(id).userId(userId).auditedAt(Instant.now()).action(AuditActionType.APPROVE)
				.entityType(AuditEntityType.BOOK_REQUEST).entityId(1L).detail(java.util.Collections.emptyMap())
				.build();
	}

	@Test
	public void findNextPageAttachesUserEmailLookedUpByUserId() {
		AuditLog fromAlex = logByUser(1L, 10L);
		AuditLog fromBrian = logByUser(2L, 20L);
		when(auditLogMapper.findNext(null, 21)).thenReturn(List.of(fromAlex, fromBrian));
		User alex = User.builder().id(10L).email("alex@example.com").build();
		User brian = User.builder().id(20L).email("brian@example.com").build();
		when(userMapper.findByIds(List.of(10L, 20L))).thenReturn(List.of(alex, brian));

		CursorPage<AuditLogSummary> page = auditService.findNextPage(null, 20);

		assertEquals(2, page.content().size());
		assertEquals("alex@example.com", page.content().get(0).getUserEmail());
		assertEquals("brian@example.com", page.content().get(1).getUserEmail());
	}

	@Test
	public void findNextPageOnlyQueriesDistinctUserIdsOnce() {
		AuditLog first = logByUser(1L, 10L);
		AuditLog second = logByUser(2L, 10L);
		when(auditLogMapper.findNext(null, 21)).thenReturn(List.of(first, second));
		User alex = User.builder().id(10L).email("alex@example.com").build();
		when(userMapper.findByIds(List.of(10L))).thenReturn(List.of(alex));

		CursorPage<AuditLogSummary> page = auditService.findNextPage(null, 20);

		assertTrue(page.content().stream().allMatch(summary -> "alex@example.com".equals(summary.getUserEmail())));
	}

	@Test
	public void findNextPageReturnsEmptyListWithoutQueryingUsersWhenNoRows() {
		when(auditLogMapper.findNext(null, 21)).thenReturn(List.of());

		CursorPage<AuditLogSummary> page = auditService.findNextPage(null, 20);

		assertEquals(0, page.content().size());
	}
}
