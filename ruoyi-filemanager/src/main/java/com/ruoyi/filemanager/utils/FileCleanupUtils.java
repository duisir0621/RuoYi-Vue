package com.ruoyi.filemanager.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ruoyi.common.utils.file.FileUtils;

/**
 * 文件清理工具类，用于处理那些由于系统锁定或其他原因无法立即删除的文件
 * 
 * @author ruoyi
 */
public class FileCleanupUtils {
    
    private static final Logger log = LoggerFactory.getLogger(FileCleanupUtils.class);
    
    // 存储待删除的文件路径及其尝试次数
    private static final ConcurrentHashMap<String, Integer> pendingDeleteFiles = new ConcurrentHashMap<>();
    
    // 最大尝试删除次数
    private static final int MAX_ATTEMPTS = 3;
    
    // 清理任务执行间隔（分钟）
    private static final int CLEANUP_INTERVAL_MINUTES = 10;
    
    private static ScheduledExecutorService scheduler;
    
    static {
        // 初始化定时任务，定期尝试清理文件
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupPendingFiles();
            } catch (Exception e) {
                log.error("执行文件清理任务时出错", e);
            }
        }, CLEANUP_INTERVAL_MINUTES, CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }
    
    /**
     * 添加文件到待清理列表
     * 
     * @param filePath 文件路径
     */
    public static void addFileToCleanupQueue(String filePath) {
        pendingDeleteFiles.put(filePath, 0);
        log.info("文件已添加到清理队列: {}", filePath);
    }
    
    /**
     * 清理待处理的文件
     */
    public static void cleanupPendingFiles() {
        if (pendingDeleteFiles.isEmpty()) {
            return;
        }
        
        log.info("开始执行文件清理任务，当前待清理文件数: {}", pendingDeleteFiles.size());
        
        // 获取要删除的文件列表的副本，避免并发修改异常
        List<String> filesToProcess = new ArrayList<>(pendingDeleteFiles.keySet());
        
        for (String filePath : filesToProcess) {
            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    // 文件已不存在，从列表中移除
                    pendingDeleteFiles.remove(filePath);
                    log.info("文件已不存在，从清理队列移除: {}", filePath);
                    continue;
                }
                
                // 尝试删除文件
                boolean deleted = false;
                
                // 首先尝试使用File.delete()
                deleted = file.delete();
                
                // 如果失败，尝试使用nio的Files.delete()
                if (!deleted) {
                    try {
                        Path path = Paths.get(filePath);
                        Files.delete(path);
                        deleted = true;
                    } catch (Exception e) {
                        log.debug("使用Files.delete()删除文件失败: {}, 错误: {}", filePath, e.getMessage());
                    }
                }
                
                if (deleted) {
                    // 成功删除，从待处理列表中移除
                    pendingDeleteFiles.remove(filePath);
                    log.info("文件清理成功: {}", filePath);
                } else {
                    // 更新尝试次数
                    int attempts = pendingDeleteFiles.getOrDefault(filePath, 0) + 1;
                    if (attempts >= MAX_ATTEMPTS) {
                        // 达到最大尝试次数，从列表中移除
                        pendingDeleteFiles.remove(filePath);
                        log.warn("文件删除失败达到最大尝试次数，请手动检查: {}", filePath);
                    } else {
                        pendingDeleteFiles.put(filePath, attempts);
                        log.info("文件删除尝试失败，将在下次尝试: {}, 当前尝试次数: {}", filePath, attempts);
                    }
                }
            } catch (Exception e) {
                log.error("清理文件时发生异常: {}, 错误: {}", filePath, e.getMessage(), e);
            }
        }
        
        log.info("文件清理任务完成，剩余待清理文件数: {}", pendingDeleteFiles.size());
    }
    
    /**
     * 关闭清理任务调度器
     */
    public static void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
} 