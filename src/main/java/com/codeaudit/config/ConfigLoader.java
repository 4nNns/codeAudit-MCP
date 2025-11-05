package com.codeaudit.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

/**
 * 配置加载器
 */
public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static final String DEFAULT_CONFIG_PATH = "resources/config.yaml";

    /**
     * 加载配置文件
     */
    @SuppressWarnings("unchecked")
    public static Config loadConfig(String configPath) throws FileNotFoundException {
        if (configPath == null || configPath.isEmpty()) {
            configPath = DEFAULT_CONFIG_PATH;
        }

        Yaml yaml = new Yaml();
        try (InputStream inputStream = new FileInputStream(configPath)) {
            Map<String, Object> data = yaml.load(inputStream);
            Config config = mapToConfig(data);
            logger.info("配置文件加载成功: {}", configPath);
            return config;
        } catch (FileNotFoundException e) {
            logger.error("配置文件未找到: {}", configPath);
            throw e;
        } catch (Exception e) {
            logger.error("加载配置文件失败: {}", configPath, e);
            throw new RuntimeException("加载配置文件失败", e);
        }
    }

    /**
     * 将 Map 转换为 Config 对象
     */
    @SuppressWarnings("unchecked")
    private static Config mapToConfig(Map<String, Object> data) {
        Config config = new Config();
        
        // 处理 code_audit
        if (data.containsKey("code_audit")) {
            Map<String, Object> codeAuditMap = (Map<String, Object>) data.get("code_audit");
            Config.CodeAuditConfig codeAudit = new Config.CodeAuditConfig();
            
            if (codeAuditMap.containsKey("repository_path")) {
                codeAudit.setRepositoryPath((String) codeAuditMap.get("repository_path"));
            }
            
            if (codeAuditMap.containsKey("ast_cache")) {
                Map<String, Object> astCacheMap = (Map<String, Object>) codeAuditMap.get("ast_cache");
                Config.CodeAuditConfig.ASTCacheConfig astCache = new Config.CodeAuditConfig.ASTCacheConfig();
                
                if (astCacheMap.containsKey("enabled")) {
                    astCache.setEnabled((Boolean) astCacheMap.get("enabled"));
                }
                if (astCacheMap.containsKey("cache_dir")) {
                    astCache.setCacheDir((String) astCacheMap.get("cache_dir"));
                }
                if (astCacheMap.containsKey("rebuild_on_startup")) {
                    astCache.setRebuildOnStartup((Boolean) astCacheMap.get("rebuild_on_startup"));
                }
                
                codeAudit.setAstCache(astCache);
            }
            
            config.setCodeAudit(codeAudit);
        }
        
        // 处理 remote_repository
        if (data.containsKey("remote_repository")) {
            Map<String, Object> remoteRepoMap = (Map<String, Object>) data.get("remote_repository");
            Config.RemoteRepositoryConfig remoteRepo = new Config.RemoteRepositoryConfig();
            
            if (remoteRepoMap.containsKey("enabled")) {
                remoteRepo.setEnabled((Boolean) remoteRepoMap.get("enabled"));
            }
            if (remoteRepoMap.containsKey("type")) {
                remoteRepo.setType((String) remoteRepoMap.get("type"));
            }
            if (remoteRepoMap.containsKey("url")) {
                remoteRepo.setUrl((String) remoteRepoMap.get("url"));
            }
            if (remoteRepoMap.containsKey("branch")) {
                remoteRepo.setBranch((String) remoteRepoMap.get("branch"));
            }
            if (remoteRepoMap.containsKey("target_path")) {
                remoteRepo.setTargetPath((String) remoteRepoMap.get("target_path"));
            }
            if (remoteRepoMap.containsKey("auto_clean")) {
                remoteRepo.setAutoClean((Boolean) remoteRepoMap.get("auto_clean"));
            }
            
            config.setRemoteRepository(remoteRepo);
        }
        
        return config;
    }

    /**
     * 加载默认配置文件
     */
    public static Config loadDefaultConfig() {
        try {
            return loadConfig(DEFAULT_CONFIG_PATH);
        } catch (FileNotFoundException e) {
            logger.warn("默认配置文件不存在，创建默认配置");
            return createDefaultConfig();
        }
    }

    /**
     * 创建默认配置
     */
    private static Config createDefaultConfig() {
        Config config = new Config();
        config.getCodeAudit().setRepositoryPath("");
        config.getCodeAudit().getAstCache().setEnabled(true);
        config.getCodeAudit().getAstCache().setCacheDir("./cache");
        config.getCodeAudit().getAstCache().setRebuildOnStartup(false);
        return config;
    }
}
