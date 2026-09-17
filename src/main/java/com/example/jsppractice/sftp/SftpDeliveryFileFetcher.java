package com.example.jsppractice.sftp;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.example.jsppractice.exception.SftpOperationException;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

// 每個方法各自開一條新的 SFTP 連線、用完就關掉，不維持長連線——這是排程一天跑一次的批次工作，
// 不是高頻呼叫的 API，不需要連線池那種複雜度。
public class SftpDeliveryFileFetcher implements DeliveryFileFetcher {

	private static final String PROCESSED_SUBDIR = "processed";
	private static final String FAILED_SUBDIR = "failed";

	private final String host;
	private final int port;
	private final String username;
	private final String password;
	private final String remoteDir;

	public SftpDeliveryFileFetcher(String host, int port, String username, String password, String remoteDir) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.remoteDir = remoteDir;
	}

	@Override
	public List<String> listPendingFileNames() {
		return withChannel(channel -> {
			List<String> fileNames = new ArrayList<>();
			for (ChannelSftp.LsEntry entry : channel.ls(remoteDir)) {
				if (!entry.getAttrs().isDir()) {
					fileNames.add(entry.getFilename());
				}
			}
			return fileNames;
		});
	}

	@Override
	public byte[] download(String fileName) {
		return withChannel(channel -> {
			try (InputStream in = channel.get(remoteDir + "/" + fileName)) {
				return in.readAllBytes();
			}
		});
	}

	@Override
	public void markProcessed(String fileName) {
		moveTo(fileName, PROCESSED_SUBDIR);
	}

	@Override
	public void markFailed(String fileName) {
		moveTo(fileName, FAILED_SUBDIR);
	}

	private void moveTo(String fileName, String subdir) {
		withChannel(channel -> {
			String targetDir = remoteDir + "/" + subdir;
			ensureDirectoryExists(channel, targetDir);
			channel.rename(remoteDir + "/" + fileName, targetDir + "/" + fileName);
			return null;
		});
	}

	private void ensureDirectoryExists(ChannelSftp channel, String dir) throws SftpException {
		try {
			channel.stat(dir);
		} catch (SftpException e) {
			channel.mkdir(dir);
		}
	}

	private <T> T withChannel(SftpAction<T> action) {
		Session session = null;
		ChannelSftp channel = null;
		try {
			session = new JSch().getSession(username, host, port);
			session.setPassword(password);
			// 只連自己控制的 localhost 練習用 server，跳過主機金鑰驗證；連真正的廠商 SFTP
			// 要改用 session.setKnownHosts(...) 驗證主機金鑰，不能整個關掉
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();
			channel = (ChannelSftp) session.openChannel("sftp");
			channel.connect();
			return action.run(channel);
		} catch (JSchException | SftpException | IOException e) {
			throw new SftpOperationException("SFTP 操作失敗：" + e.getMessage(), e);
		} finally {
			if (channel != null) {
				channel.disconnect();
			}
			if (session != null) {
				session.disconnect();
			}
		}
	}

	@FunctionalInterface
	private interface SftpAction<T> {
		T run(ChannelSftp channel) throws SftpException, IOException;
	}
}
