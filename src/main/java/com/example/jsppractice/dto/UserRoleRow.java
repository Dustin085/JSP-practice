package com.example.jsppractice.dto;

import com.example.jsppractice.model.RoleType;

// 批次查多個使用者的角色用（避免 findAll() 顯示使用者列表時對每一列各查一次 findRolesByUserId 造成 N+1）。
// userId/role 兩個欄位都會重複出現在多列裡（一個使用者可能有好幾個角色），查回來後在 Java 端
// 用 Collectors.groupingBy(userId) 分組——跟 audit_logs 批次查 email 是同一種 deferred join 手法。
public record UserRoleRow(Long userId, RoleType role) {
}
