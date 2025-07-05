package com.ruoyi.filemanager.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.filemanager.domain.SysFile;
import com.ruoyi.common.core.domain.AjaxResult;

/**
 * 文件管理Service接口
 * 
 * @author ruoyi
 */
public interface ISysFileService 
{
    /**
     * 查询文件
     * 
     * @param fileId 文件ID
     * @return 文件信息
     */
    public SysFile selectSysFileById(Long fileId);

    /**
     * 查询文件列表
     * 
     * @param sysFile 文件信息
     * @return 文件集合
     */
    public List<SysFile> selectSysFileList(SysFile sysFile);

    /**
     * 新增文件
     * 
     * @param sysFile 文件信息
     * @return 结果
     */
    public int insertSysFile(SysFile sysFile);

    /**
     * 修改文件
     * 
     * @param sysFile 文件信息
     * @return 结果
     */
    public int updateSysFile(SysFile sysFile);

    /**
     * 删除文件信息
     * 
     * @param fileId 文件ID
     * @return 结果
     */
    public int deleteSysFileById(Long fileId);

    /**
     * 批量删除文件信息
     * 
     * @param fileIds 需要删除的文件ID
     * @return 结果
     */
    public int deleteSysFileByIds(Long[] fileIds);
    
    /**
     * 上传文件
     * 
     * @param file 上传的文件
     * @return 文件信息
     */
    public AjaxResult uploadFile(MultipartFile file);
    
    /**
     * 下载文件
     * 
     * @param fileId 文件ID
     */
    public void downloadFile(Long fileId);
    
    /**
     * 批量下载文件
     * 
     * @param fileIds 文件ID数组
     */
    public void batchDownloadFile(Long[] fileIds);
} 