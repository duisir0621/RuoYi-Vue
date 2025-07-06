package com.ruoyi.filemanager.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileTypeUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.filemanager.domain.SysFile;
import com.ruoyi.filemanager.mapper.SysFileMapper;
import com.ruoyi.filemanager.service.ISysFileService;
import com.ruoyi.filemanager.utils.FileCleanupUtils;
import com.ruoyi.filemanager.utils.FileManagerUtils;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import javax.annotation.PreDestroy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;

/**
 * 文件管理Service业务层处理
 * 
 * @author ruoyi
 */
@Service
public class SysFileServiceImpl implements ISysFileService 
{
    private static final Logger log = LoggerFactory.getLogger(SysFileServiceImpl.class);

    @Autowired
    private SysFileMapper sysFileMapper;
    
    @Autowired
    private HttpServletRequest request;
    
    @Autowired
    private HttpServletResponse response;
    
    // 添加ThreadLocal存储最近处理的文件ID和时间戳
    private static final ThreadLocal<Map<Long, Long>> RECENT_DOWNLOADS = ThreadLocal.withInitial(HashMap::new);
    
    // 设置防重复下载的时间窗口（毫秒）
    private static final long DOWNLOAD_THRESHOLD = 2000;

    /**
     * 查询文件
     * 
     * @param fileId 文件ID
     * @return 文件信息
     */
    @Override
    public SysFile selectSysFileById(Long fileId)
    {
        return sysFileMapper.selectSysFileById(fileId);
    }

    /**
     * 查询文件列表
     * 
     * @param sysFile 文件信息
     * @return 文件集合
     */
    @Override
    public List<SysFile> selectSysFileList(SysFile sysFile)
    {
        return sysFileMapper.selectSysFileList(sysFile);
    }

    /**
     * 新增文件
     * 
     * @param sysFile 文件信息
     * @return 结果
     */
    @Override
    public int insertSysFile(SysFile sysFile)
    {
        sysFile.setCreateBy(SecurityUtils.getUsername());
        sysFile.setCreateTime(DateUtils.getNowDate());
        return sysFileMapper.insertSysFile(sysFile);
    }

    /**
     * 修改文件
     * 
     * @param sysFile 文件信息
     * @return 结果
     */
    @Override
    public int updateSysFile(SysFile sysFile)
    {
        sysFile.setUpdateBy(SecurityUtils.getUsername());
        sysFile.setUpdateTime(DateUtils.getNowDate());
        return sysFileMapper.updateSysFile(sysFile);
    }

    /**
     * 删除文件信息
     * 
     * @param fileId 文件ID
     * @return 结果
     */
    @Override
    public int deleteSysFileById(Long fileId)
    {
        SysFile sysFile = sysFileMapper.selectSysFileById(fileId);
        if (sysFile != null && StringUtils.isNotEmpty(sysFile.getFilePath()))
        {
            String filePath = RuoYiConfig.getProfile() + StringUtils.substringAfter(sysFile.getFilePath(), "/profile");
            
            // 记录删除前的信息
            log.info("准备删除文件 - ID: {}, 路径: {}, 类型: {}", fileId, filePath, sysFile.getFileType());
            
            // 尝试删除物理文件
            boolean deleteResult = FileUtils.deleteFile(filePath);
            
            // 记录删除结果
            if (deleteResult) {
                log.info("物理文件删除成功 - ID: {}", fileId);
            } else {
                log.warn("物理文件删除失败 - ID: {}, 路径: {}, 将加入清理队列", fileId, filePath);
                
                // 对于特定类型的文件（PDF和压缩包），如果常规删除失败，添加到清理队列
                if (sysFile.getFileType() != null && 
                    (sysFile.getFileType().equalsIgnoreCase("pdf") || 
                     sysFile.getFileType().equalsIgnoreCase("zip") || 
                     sysFile.getFileType().equalsIgnoreCase("rar") ||
                     sysFile.getFileType().equalsIgnoreCase("7z"))) 
                {
                    File fileToDelete = new File(filePath);
                    if (fileToDelete.exists()) {
                        try {
                            // 添加到清理队列
                            FileCleanupUtils.addFileToCleanupQueue(filePath);
                            
                            // 同时设置为JVM退出时删除，双重保险
                            fileToDelete.deleteOnExit();
                            log.info("文件已添加到清理队列并设置为JVM退出时删除 - ID: {}", fileId);
                        } catch (Exception e) {
                            log.error("设置文件清理失败 - ID: {}, 错误: {}", fileId, e.getMessage(), e);
                        }
                    }
                }
            }
        }
        
        // 无论物理文件是否删除成功，都删除数据库记录
        return sysFileMapper.deleteSysFileById(fileId);
    }

    /**
     * 批量删除文件信息
     * 
     * @param fileIds 需要删除的文件ID
     * @return 结果
     */
    @Override
    public int deleteSysFileByIds(Long[] fileIds)
    {
        log.info("开始批量删除文件，共 {} 个文件", fileIds.length);
        int successCount = 0;
        for (Long fileId : fileIds)
        {
            try {
                int result = deleteSysFileById(fileId);
                if (result > 0) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("删除文件失败 - ID: {}, 错误: {}", fileId, e.getMessage(), e);
            }
        }
        log.info("批量删除完成，成功: {}, 失败: {}", successCount, (fileIds.length - successCount));
        return successCount;
    }
    
    /**
     * 上传文件
     * 
     * @param file 上传的文件
     * @return 文件信息
     */
    @Override
    public AjaxResult uploadFile(MultipartFile file)
    {
        try
        {
            // 上传文件路径
            String filePath = RuoYiConfig.getUploadPath();
            // 上传并返回新文件名称（包含路径）
            String filePathName = FileManagerUtils.uploadFile(filePath, file);
            // 获取不包含路径的文件名
            String fileName = StringUtils.substringAfterLast(filePathName, "/");
            
            SysFile sysFile = new SysFile();
            sysFile.setFileName(fileName);
            sysFile.setOriginalName(file.getOriginalFilename());
            sysFile.setFilePath(filePathName);
            sysFile.setFileType(FileTypeUtils.getFileType(file.getOriginalFilename()));
            sysFile.setFileSize(file.getSize() / 1024); // 转为KB
            sysFile.setStatus("0");
            sysFile.setDownloadCount(0L);
            sysFile.setDeptId(SecurityUtils.getDeptId());
            
            insertSysFile(sysFile);
            
            AjaxResult ajax = AjaxResult.success();
            ajax.put("fileName", fileName);
            ajax.put("url", filePathName);
            ajax.put("fileId", sysFile.getFileId());
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }
    
    /**
     * 下载文件
     * 
     * @param fileId 文件ID
     */
    @Override
    public void downloadFile(Long fileId)
    {
        // 检查是否是短时间内的重复下载请求
        Map<Long, Long> recentDownloads = RECENT_DOWNLOADS.get();
        Long lastDownloadTime = recentDownloads.get(fileId);
        long currentTime = System.currentTimeMillis();
        
        if (lastDownloadTime != null && (currentTime - lastDownloadTime) < DOWNLOAD_THRESHOLD) {
            log.info("检测到重复下载请求，文件ID: {}, 上次下载时间: {}, 当前时间: {}", fileId, lastDownloadTime, currentTime);
            try {
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"msg\":\"请勿重复下载\",\"code\":500}");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // 记录本次下载时间
        recentDownloads.put(fileId, currentTime);
        
        SysFile sysFile = sysFileMapper.selectSysFileById(fileId);
        if (sysFile == null)
        {
            try {
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"msg\":\"文件不存在\",\"code\":500}");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        FileInputStream fileInputStream = null;
        try
        {
            String filePath = RuoYiConfig.getProfile() + StringUtils.substringAfter(sysFile.getFilePath(), "/profile");
            
            // 检查文件是否存在
            File file = new File(filePath);
            if (!file.exists())
            {
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"msg\":\"文件不存在\",\"code\":500}");
                return;
            }
            
            // 确保设置正确的MIME类型，特别是PDF文件
            String fileType = sysFile.getFileType();
            String contentType = MimeTypeUtils.getContentType(fileType);
            
            // 记录日志信息，帮助调试
            log.info("开始下载文件 - ID: {}, 类型: {}, 内容类型: {}, 大小: {}KB", 
                    fileId, fileType, contentType, file.length()/1024);
            
            response.reset(); // 重置响应对象，清除缓存
            
            // 明确为PDF文件设置正确的Content-Type
            boolean isPdf = "pdf".equalsIgnoreCase(fileType);

            if (isPdf) {
                contentType = MimeTypeUtils.PDF;
                response.setContentType(MimeTypeUtils.PDF);
            } else {
                response.setContentType(contentType);
            }
            
            response.setCharacterEncoding("utf-8");
            
            // 直接使用原始文件名作为下载名称，确保不包含路径
            String downloadName = sysFile.getOriginalName();
            // 如果原始文件名中包含路径分隔符，则只取文件名部分
            if (downloadName.contains("/") || downloadName.contains("\\")) {
                downloadName = StringUtils.substringAfterLast(downloadName, downloadName.contains("/") ? "/" : "\\");
            }
            
            // 确保PDF文件有.pdf后缀
            if (isPdf && !downloadName.toLowerCase().endsWith(".pdf")) {
                downloadName = downloadName + ".pdf";
            }
            
            try {
                // 使用UTF-8编码处理文件名
                String encodeFilename = URLEncoder.encode(downloadName, StandardCharsets.UTF_8.toString());
                // 处理空格等特殊字符
                encodeFilename = encodeFilename.replaceAll("\\+", "%20");
                
                // 设置响应头，兼容各种浏览器
                String userAgent = request.getHeader("User-Agent");
                
                // 对于PDF文件，明确使用attachment作为disposition类型，确保浏览器下载而不是在线查看
                String disposition = "attachment";
                
                // 为不同浏览器设置不同的响应头
                if (userAgent != null && (userAgent.contains("MSIE") || userAgent.contains("Trident"))) {
                    // IE浏览器
                    response.setHeader("Content-Disposition", disposition + ";filename=" + encodeFilename);
                } else if (userAgent != null && userAgent.contains("Firefox")) {
                    // 火狐浏览器
                    response.setHeader("Content-Disposition", disposition + ";filename*=UTF-8''" + encodeFilename);
                } else {
                    // Chrome、Edge等浏览器
                    response.setHeader("Content-Disposition", disposition + ";filename=" + encodeFilename);
                }
                
                // 添加额外的响应头，避免缓存和CORS问题
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
                response.setHeader("Access-Control-Allow-Origin", "*");
                response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
                
                // 设置内容长度，避免传输问题
                response.setHeader("Content-Length", String.valueOf(file.length()));
                
            } catch (UnsupportedEncodingException e) {
                // 如果编码失败，使用默认方法
                response.setHeader("Content-disposition", "attachment;filename=" + FileUtils.setFileDownloadHeader(request, downloadName));
            }
            
            // 优化：针对大文件采用更大的缓冲区
            fileInputStream = new FileInputStream(file);
            long fileSize = file.length();
            
            // 针对大小文件使用不同的缓冲区大小
            int bufferSize = fileSize > 10 * 1024 * 1024 ? 1024 * 1024 : 1024 * 32; // 大文件用1MB缓冲，小文件用32KB
            byte[] buffer = new byte[bufferSize]; 
            OutputStream outputStream = response.getOutputStream();

            // 优化：配置文件读写超时
            try {
                // 尝试延长Tomcat的Socket超时时间
                if (fileSize > 10 * 1024 * 1024) { // 大于10MB的文件
                    log.info("下载较大文件({}MB)，已配置延长超时时间", fileSize / (1024 * 1024));
                }

                int bytesRead;
                int totalRead = 0;
                long startTime = System.currentTimeMillis();
                int flushThreshold = bufferSize * 2; // 每读取两个缓冲区大小的数据就刷新一次
                int accumulatedBytes = 0;
                
                // 分块传输文件数据
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                    accumulatedBytes += bytesRead;
                    
                    // 定期刷新输出流以防止缓冲区溢出
                    if (accumulatedBytes >= flushThreshold) {
                        outputStream.flush();
                        accumulatedBytes = 0;
                        
                        // 记录下载进度
                        if (totalRead % (5 * 1024 * 1024) < bufferSize) { // 每下载5MB记录一次进度
                            long timeElapsed = System.currentTimeMillis() - startTime;
                            if (timeElapsed > 0) {
                                double speedMBps = (totalRead / 1024.0 / 1024.0) / (timeElapsed / 1000.0);
                                log.debug("文件下载进度 - ID: {}, 已下载: {}MB, 总大小: {}MB, 速度: {:.2f}MB/s", 
                                        fileId, totalRead / (1024 * 1024), fileSize / (1024 * 1024), speedMBps);
                            }
                        }
                    }
                }
                
                // 确保剩余数据刷新
                outputStream.flush();
                
                // 更新下载次数
                sysFileMapper.updateSysFileDownloadCount(fileId);
                
                long timeTotal = System.currentTimeMillis() - startTime;
                log.info("文件下载完成 - ID: {}, 总大小: {}KB, 耗时: {}ms", fileId, fileSize / 1024, timeTotal);
                
            } catch (SocketTimeoutException ste) {
                log.error("下载文件时连接超时: {}", ste.getMessage());
                // 连接已超时，不再尝试写入响应
            } catch (IOException ioe) {
                // 检查是否是客户端中断连接（常见情况）
                if (ioe.getMessage() != null && 
                    (ioe.getMessage().contains("Broken pipe") || 
                     ioe.getMessage().contains("Connection reset") ||
                     ioe.getMessage().contains("connection was aborted"))) {
                    log.info("客户端可能已取消下载 - ID: {}, 错误: {}", fileId, ioe.getMessage());
                } else {
                    log.error("下载文件IO异常 - ID: {}, 错误: {}", fileId, ioe.getMessage());
                }
            }
        }
        catch (Exception e)
        {
            log.error("下载文件处理异常: {}", e.getMessage(), e);
            try {
                // 检查响应是否已提交，避免"Cannot call reset() after response has been committed"异常
                if (!response.isCommitted()) {
                    response.reset();
                    response.setContentType("application/json;charset=utf-8");
                    response.getWriter().write("{\"msg\":\"下载文件失败：" + e.getMessage() + "\",\"code\":500}");
                } else {
                    // 响应已提交，只记录错误，不再尝试重置响应
                    log.error("文件下载异常，响应已提交，无法重置响应: {}", e.getMessage());
                }
            } catch (IOException ioe) {
                log.error("响应错误信息失败: {}", ioe.getMessage());
            }
        }
        finally {
            // 确保关闭文件输入流
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    log.error("关闭文件输入流失败: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * 批量下载文件
     * 
     * @param fileIds 文件ID数组
     */
    @Override
    public void batchDownloadFile(Long[] fileIds)
    {
        if (fileIds == null || fileIds.length == 0)
        {
            try {
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"msg\":\"请选择要下载的文件\",\"code\":500}");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // 检查是否是短时间内的重复下载请求
        Map<Long, Long> recentDownloads = RECENT_DOWNLOADS.get();
        String downloadKey = String.join(",", Arrays.stream(fileIds).map(String::valueOf).toArray(String[]::new));
        Long lastDownloadTime = recentDownloads.get(-1L); // 使用-1作为批量下载的特殊标记
        long currentTime = System.currentTimeMillis();
        
        if (lastDownloadTime != null && (currentTime - lastDownloadTime) < DOWNLOAD_THRESHOLD) {
            log.info("检测到重复批量下载请求，文件ID: {}, 上次下载时间: {}, 当前时间: {}", downloadKey, lastDownloadTime, currentTime);
            try {
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"msg\":\"请勿重复下载\",\"code\":500}");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // 记录本次下载时间
        recentDownloads.put(-1L, currentTime);
        
        // 临时ZIP文件名
        String zipFileName = UUID.randomUUID().toString() + ".zip";
        String tempDir = System.getProperty("java.io.tmpdir");
        String zipFilePath = tempDir + File.separator + zipFileName;
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath)))
        {
            for (Long fileId : fileIds)
            {
                SysFile sysFile = sysFileMapper.selectSysFileById(fileId);
                if (sysFile == null || StringUtils.isEmpty(sysFile.getFilePath()))
                {
                    continue;
                }
                
                String filePath = RuoYiConfig.getProfile() + StringUtils.substringAfter(sysFile.getFilePath(), "/profile");
                File file = new File(filePath);
                if (!file.exists())
                {
                    continue;
                }
                
                // 获取原始文件名，避免重名文件覆盖
                String entryName = sysFile.getOriginalName();
                // 如果原始文件名中包含路径分隔符，则只取文件名部分
                if (entryName.contains("/") || entryName.contains("\\")) {
                    entryName = StringUtils.substringAfterLast(entryName, entryName.contains("/") ? "/" : "\\");
                }
                
                // 处理文件重名问题，添加文件ID作为前缀
                if (sysFile.getFileId() != null) {
                    String extension = StringUtils.substringAfterLast(entryName, ".");
                    String fileNameWithoutExt = StringUtils.substringBeforeLast(entryName, ".");
                    entryName = fileNameWithoutExt + "_" + sysFile.getFileId() + (StringUtils.isNotEmpty(extension) ? "." + extension : "");
                }
                
                // 添加到ZIP
                ZipEntry zipEntry = new ZipEntry(entryName);
                zos.putNextEntry(zipEntry);
                
                byte[] bytes = Files.readAllBytes(Paths.get(filePath));
                zos.write(bytes, 0, bytes.length);
                zos.closeEntry();
                
                // 更新下载次数
                sysFileMapper.updateSysFileDownloadCount(fileId);
            }
        }
        catch (IOException e)
        {
            try {
                // 检查响应是否已提交，避免"Cannot call reset() after response has been committed"异常
                if (!response.isCommitted()) {
                    response.setContentType("application/json;charset=utf-8");
                    response.getWriter().write("{\"msg\":\"创建压缩文件失败：" + e.getMessage() + "\",\"code\":500}");
                } else {
                    // 响应已提交，只记录错误
                    log.error("创建压缩文件失败，响应已提交: {}", e.getMessage());
                }
                return;
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        
        try
        {
            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-disposition", "attachment;filename=" + 
                    FileUtils.setFileDownloadHeader(request, "批量下载文件.zip"));
            
            // 写入响应流
            FileUtils.writeBytes(zipFilePath, response.getOutputStream());
            
            // 删除临时文件
            FileUtils.deleteFile(zipFilePath);
        }
        catch (IOException e)
        {
                        try {
                response.reset();
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"msg\":\"下载文件失败：" + e.getMessage() + "\",\"code\":500}");
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
          }
    }

    /**
     * 清理ThreadLocal资源
     * 在请求结束时调用，避免内存泄漏
     */
    @PreDestroy
    public void cleanup() {
        RECENT_DOWNLOADS.remove();
    }
    
} 