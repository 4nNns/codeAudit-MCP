package com.codeaudit.config;

/**
 * 应用配置结构
 */
public class Config {
    private CodeAuditConfig codeAudit;
    
    private RemoteRepositoryConfig remoteRepository;

    public Config() {
        this.codeAudit = new CodeAuditConfig();
        this.remoteRepository = new RemoteRepositoryConfig();
    }

    public CodeAuditConfig getCodeAudit() {
        return codeAudit;
    }

    public void setCodeAudit(CodeAuditConfig codeAudit) {
        this.codeAudit = codeAudit;
    }

    public RemoteRepositoryConfig getRemoteRepository() {
        return remoteRepository;
    }

    public void setRemoteRepository(RemoteRepositoryConfig remoteRepository) {
        this.remoteRepository = remoteRepository;
    }

    public static class CodeAuditConfig {
        private String repositoryPath;
        
        private ASTCacheConfig astCache;

        public CodeAuditConfig() {
            this.astCache = new ASTCacheConfig();
        }

        public String getRepositoryPath() {
            return repositoryPath;
        }

        public void setRepositoryPath(String repositoryPath) {
            this.repositoryPath = repositoryPath;
        }

        public ASTCacheConfig getAstCache() {
            return astCache;
        }

        public void setAstCache(ASTCacheConfig astCache) {
            this.astCache = astCache;
        }

        public static class ASTCacheConfig {
            private boolean enabled = true;
            
            private String cacheDir = "./cache";
            
            private boolean rebuildOnStartup = false;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getCacheDir() {
                return cacheDir;
            }

            public void setCacheDir(String cacheDir) {
                this.cacheDir = cacheDir;
            }

            public boolean isRebuildOnStartup() {
                return rebuildOnStartup;
            }

            public void setRebuildOnStartup(boolean rebuildOnStartup) {
                this.rebuildOnStartup = rebuildOnStartup;
            }
        }
    }

    public static class RemoteRepositoryConfig {
        private boolean enabled = false;
        
        private String type; // "zip", "git", "local"
        
        private String url;
        
        private String branch = "main";
        
        private String targetPath;
        
        private boolean autoClean = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getBranch() {
            return branch;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }

        public String getTargetPath() {
            return targetPath;
        }

        public void setTargetPath(String targetPath) {
            this.targetPath = targetPath;
        }

        public boolean isAutoClean() {
            return autoClean;
        }

        public void setAutoClean(boolean autoClean) {
            this.autoClean = autoClean;
        }
    }

    /**
     * 生成缓存文件名
     */
    public String generateCacheFileName() {
        String repoName = getRepositoryName();
        repoName = repoName.replace(" ", "_");
        return repoName + "_ast_index.json";
    }

    /**
     * 获取仓库名称
     */
    private String getRepositoryName() {
        String path = codeAudit.getRepositoryPath();
        if (path == null || path.isEmpty()) {
            return "unknown";
        }
        java.io.File file = new java.io.File(path);
        return file.getName();
    }

    /**
     * 获取完整的缓存文件路径
     */
    public String getCacheFilePath() {
        String cacheDir = codeAudit.getAstCache().getCacheDir();
        if (cacheDir == null || cacheDir.isEmpty()) {
            cacheDir = "./cache";
        }
        String cacheFileName = generateCacheFileName();
        return new java.io.File(cacheDir, cacheFileName).getPath();
    }
}
