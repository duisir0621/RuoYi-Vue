package com.ruoyi.filemanager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ruoyi.filemanager.utils.FileCleanupUtils;

/**
 * 文件管理配置类
 * 
 * @author ruoyi
 */
@Configuration
public class FileManagerConfig {
    
    private static final Logger log = LoggerFactory.getLogger(FileManagerConfig.class);
    
    /**
     * 初始化文件清理任务
     */
    @Bean
    public CommandLineRunner initFileCleanupTask() {
        return args -> {
            log.info("初始化文件清理任务");
            
            // 系统退出时，关闭文件清理调度器
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("系统关闭，停止文件清理任务");
                FileCleanupUtils.shutdown();
            }));
            
            log.info("文件清理任务初始化完成");
        };
    }
} 