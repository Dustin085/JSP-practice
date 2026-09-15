package com.example.jsppractice.util;

public final class EmailMasker {

	private EmailMasker() {
	}

	// 規則：本地部分只留第一個字（跟最後一個字，如果夠長的話），中間換成固定的 ***；
	// 網域維持原樣不遮——網域不是個資，遮了對「看不看得出是哪家公司的信箱」沒有意義。
	// 沒有 @ 的字串代表不是合法 email，保守整串遮掉。
	public static String mask(String email) {
		if (email == null) {
			return null;
		}
		int atIndex = email.indexOf('@');
		if (atIndex <= 0) {
			return "***";
		}
		String local = email.substring(0, atIndex);
		String domain = email.substring(atIndex);
		if (local.length() <= 2) {
			return local.charAt(0) + "***" + domain;
		}
		return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
	}
}
