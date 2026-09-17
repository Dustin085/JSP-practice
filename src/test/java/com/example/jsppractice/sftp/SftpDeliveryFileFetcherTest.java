package com.example.jsppractice.sftp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// 不 mock JSch，真的啟動一個內嵌的假 SFTP server（監聽本機隨機 port），驗證 SftpDeliveryFileFetcher
// 有沒有真的走完連線、認證、下載、搬檔這整套 SFTP 協定，不是只測到程式碼表面上看起來對。
public class SftpDeliveryFileFetcherTest {

	private static final String USERNAME = "testuser";
	private static final String PASSWORD = "testpass";
	private static final String REMOTE_DIR = "/delivery-notices";

	private SshServer sshServer;
	private Path rootDir;
	private Path remoteDirOnDisk;
	private SftpDeliveryFileFetcher fetcher;

	@Before
	public void setUp() throws IOException {
		rootDir = Files.createTempDirectory("sftp-test");
		remoteDirOnDisk = rootDir.resolve("delivery-notices");
		Files.createDirectories(remoteDirOnDisk);

		sshServer = SshServer.setUpDefaultServer();
		sshServer.setHost("localhost");
		sshServer.setPort(0);
		sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
		sshServer.setPasswordAuthenticator(
				(username, password, session) -> USERNAME.equals(username) && PASSWORD.equals(password));
		sshServer.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
		sshServer.setFileSystemFactory(new VirtualFileSystemFactory(rootDir));
		sshServer.start();

		fetcher = new SftpDeliveryFileFetcher("localhost", sshServer.getPort(), USERNAME, PASSWORD, REMOTE_DIR);
	}

	@After
	public void tearDown() throws IOException {
		sshServer.stop();
		deleteRecursively(rootDir);
	}

	private static void deleteRecursively(Path path) throws IOException {
		if (!Files.exists(path)) {
			return;
		}
		try (var walk = Files.walk(path)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (IOException e) {
					// 測試結束後的清理，刪不掉不影響測試結果本身
				}
			});
		}
	}

	@Test
	public void listPendingFileNamesReturnsFilesInRemoteDirectory() throws IOException {
		Files.writeString(remoteDirOnDisk.resolve("20260916.txt"), "content");

		List<String> fileNames = fetcher.listPendingFileNames();

		assertEquals(List.of("20260916.txt"), fileNames);
	}

	@Test
	public void listPendingFileNamesExcludesSubdirectories() throws IOException {
		Files.createDirectories(remoteDirOnDisk.resolve("processed"));
		Files.writeString(remoteDirOnDisk.resolve("file.txt"), "content");

		List<String> fileNames = fetcher.listPendingFileNames();

		assertEquals(List.of("file.txt"), fileNames);
	}

	@Test
	public void downloadReturnsFileContentBytes() throws IOException {
		byte[] content = "hello delivery".getBytes(StandardCharsets.UTF_8);
		Files.write(remoteDirOnDisk.resolve("file.txt"), content);

		byte[] downloaded = fetcher.download("file.txt");

		assertArrayEquals(content, downloaded);
	}

	@Test
	public void markProcessedMovesFileIntoProcessedSubdirectory() throws IOException {
		Files.writeString(remoteDirOnDisk.resolve("file.txt"), "content");

		fetcher.markProcessed("file.txt");

		assertFalse(Files.exists(remoteDirOnDisk.resolve("file.txt")));
		assertTrue(Files.exists(remoteDirOnDisk.resolve("processed").resolve("file.txt")));
	}

	@Test
	public void markFailedMovesFileIntoFailedSubdirectory() throws IOException {
		Files.writeString(remoteDirOnDisk.resolve("file.txt"), "content");

		fetcher.markFailed("file.txt");

		assertFalse(Files.exists(remoteDirOnDisk.resolve("file.txt")));
		assertTrue(Files.exists(remoteDirOnDisk.resolve("failed").resolve("file.txt")));
	}
}
