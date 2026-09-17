package com.example.jsppractice.sftp;

import java.util.List;

public interface DeliveryFileFetcher {
	List<String> listPendingFileNames();

	byte[] download(String fileName);

	void markProcessed(String fileName);

	void markFailed(String fileName);
}
