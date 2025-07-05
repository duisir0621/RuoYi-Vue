package com.ruoyi.filemanager.mapper;

import java.util.List;
import com.ruoyi.filemanager.domain.SysFile;

/**
 * 文件管理Mapper接口
 * 
 * @author ruoyi
 */
public interface SysFileMapper 
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
     * 删除文件
     * 
     * @param fileId 文件ID
     * @return 结果
     */
    public int deleteSysFileById(Long fileId);

    /**
     * 批量删除文件
     * 
     * @param fileIds 需要删除的文件ID
     * @return 结果
     */
    public int deleteSysFileByIds(Long[] fileIds);
    
    /**
     * 更新文件下载次数
     * 
     * @param fileId 文件ID
     * @return 结果
     */
    public int updateSysFileDownloadCount(Long fileId);
} 