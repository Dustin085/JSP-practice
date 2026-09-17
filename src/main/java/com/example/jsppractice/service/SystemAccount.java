package com.example.jsppractice.service;

// 給排程/批次這類沒有人工操作者的流程寫 audit_logs 用（user_id 是 NOT NULL 外鍵，不能留空）。
// 這個帳號不會被拿去登入，DataSeeder 用隨機密碼建立它，沒有人知道這組密碼。
public final class SystemAccount {

	public static final String EMAIL = "system@jsppractice.internal";

	private SystemAccount() {
	}
}
