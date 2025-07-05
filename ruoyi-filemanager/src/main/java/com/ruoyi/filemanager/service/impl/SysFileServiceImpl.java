package com.ruoyi.filemanager.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import com.ruoyi.filemanager.utils.FileManagerUtils;

/**
 * 文件管理Service业务层处理
 * 
 * @author ruoyi
 */
@Service
public class SysFileServiceImpl implements ISysFileService 
{
    @Autowired
    private SysFileMapper sysFileMapper;
    
    @Autowired
    private HttpServletRequest request;
    
    @Autowired
    private HttpServletResponse response;

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
            FileUtils.deleteFile(filePath);
        }
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
        for (Long fileId : fileIds)
        {
            deleteSysFileById(fileId);
        }
        return fileIds.length;
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
     * @return 结果
     */
    @Override
    public AjaxResult downloadFile(Long fileId)
    {
        SysFile sysFile = sysFileMapper.selectSysFileById(fileId);
        if (sysFile == null)
        {
            return AjaxResult.error("文件不存在");
        }
        
        try
        {
            String filePath = RuoYiConfig.getProfile() + StringUtils.substringAfter(sysFile.getFilePath(), "/profile");
            
            // 检查文件是否存在
            File file = new File(filePath);
            if (!file.exists())
            {
                return AjaxResult.error("文件不存在");
            }
            
            response.setContentType(MimeTypeUtils.getContentType(sysFile.getFileType()));
            response.setCharacterEncoding("utf-8");
            
            // 直接使用原始文件名作为下载名称，确保不包含路径
            String downloadName = sysFile.getOriginalName();
            // 如果原始文件名中包含路径分隔符，则只取文件名部分
            if (downloadName.contains("/") || downloadName.contains("\\")) {
                downloadName = StringUtils.substringAfterLast(downloadName, downloadName.contains("/") ? "/" : "\\");
            }
            response.setHeader("Content-disposition", "attachment;filename=" + FileUtils.setFileDownloadHeader(request, downloadName));
            
            FileUtils.writeBytes(filePath, response.getOutputStream());
            
            // 更新下载次数
            sysFileMapper.updateSysFileDownloadCount(fileId);
            
            return AjaxResult.success();
        }
        catch (IOException e)
        {
            return AjaxResult.error("下载文件失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量下载文件
     * 
     * @param fileIds 文件ID数组
     * @return 结果
     */
    @Override
    public AjaxResult batchDownloadFile(Long[] fileIds)
    {
        if (fileIds == null || fileIds.length == 0)
        {
            return AjaxResult.error("请选择要下载的文件");
        }
        
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
            return AjaxResult.error("创建压缩文件失败：" + e.getMessage());
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
            
            return AjaxResult.success();
        }
        catch (IOException e)
        {
            return AjaxResult.error("下载文件失败：" + e.getMessage());
        }
    }
} 