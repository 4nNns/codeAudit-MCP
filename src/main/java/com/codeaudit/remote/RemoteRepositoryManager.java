package com.codeaudit.remote;

import com.codeaudit.config.Config;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 远程仓库管理器
 */
public class RemoteRepositoryManager {
    private static final Logger logger = LoggerFactory.getLogger(RemoteRepositoryManager.class);
    private final Config config;

    public RemoteRepositoryManager(Config config) {
        this.config = config;
    }

    /**
     * 下载并准备远程仓库
     */
    public String downloadAndPrepare() throws Exception {
        var repoConfig = config.getRemoteRepository();

        switch (repoConfig.getType()) {
            case "zip":
                return downloadAndExtractZip(repoConfig.getUrl(), repoConfig.getTargetPath());
            case "git":
                return cloneGitRepository(repoConfig.getUrl(), repoConfig.getBranch(), repoConfig.getTargetPath());
            case "local":
                return repoConfig.getTargetPath();
            default:
                throw new IllegalArgumentException("不支持的仓库类型: " + repoConfig.getType());
        }
    }

    /**
     * 下载并解压ZIP文件
     */
    private String downloadAndExtractZip(String url, String targetPath) throws Exception {
        logger.info("开始下载: {}", url);

        // 清理目标目录
        Path target = Paths.get(targetPath);
        if (Files.exists(target)) {
            deleteDirectory(target);
        }
        Files.createDirectories(target);

        // 下载文件
        String zipPath = Paths.get(targetPath, "download.zip").toString();
        downloadFile(url, zipPath);

        // 解压文件
        extractZip(zipPath, targetPath);

        // 删除zip文件
        Files.deleteIfExists(Paths.get(zipPath));

        logger.info("下载和解压完成: {}", targetPath);
        return targetPath;
    }

    /**
     * 下载文件
     */
    private void downloadFile(String urlString, String filePath) throws Exception {
        URL url = new URL(urlString);
        try (InputStream in = url.openStream();
             FileOutputStream out = new FileOutputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        logger.info("下载完成: {}", filePath);
    }

    /**
     * 解压ZIP文件
     */
    private void extractZip(String zipPath, String targetPath) throws Exception {
        logger.info("开始解压: {}", zipPath);

        Path target = Paths.get(targetPath);
        Path zipFile = Paths.get(zipPath);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = target.resolve(entry.getName());
                
                // 防止路径遍历攻击
                if (!entryPath.normalize().startsWith(target.normalize())) {
                    throw new SecurityException("非法文件路径: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }

        logger.info("解压完成: {}", targetPath);
    }

    /**
     * 克隆Git仓库
     */
    private String cloneGitRepository(String url, String branch, String targetPath) throws Exception {
        logger.info("开始克隆Git仓库: {} (分支: {})", url, branch);

        // 清理目标目录
        Path target = Paths.get(targetPath);
        if (Files.exists(target)) {
            deleteDirectory(target);
        }

        // 克隆仓库
        Git git = Git.cloneRepository()
                .setURI(url)
                .setDirectory(new File(targetPath))
                .setBranch(branch)
                .call();

        git.close();

        logger.info("Git克隆完成: {}", targetPath);
        return targetPath;
    }

    /**
     * 清理临时文件
     */
    public void cleanup() throws IOException {
        var repoConfig = config.getRemoteRepository();
        if (repoConfig.isAutoClean()) {
            logger.info("清理临时目录: {}", repoConfig.getTargetPath());
            Path target = Paths.get(repoConfig.getTargetPath());
            if (Files.exists(target)) {
                deleteDirectory(target);
            }
        }
    }

    /**
     * 删除目录
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (var stream = Files.walk(directory)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }
}

