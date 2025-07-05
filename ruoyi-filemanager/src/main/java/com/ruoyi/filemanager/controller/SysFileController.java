package com.ruoyi.filemanager.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.filemanager.domain.SysFile;
import com.ruoyi.filemanager.service.ISysFileService;

/**
 * 文件管理Controller
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/filemanager/file")
public class SysFileController extends BaseController
{
    @Autowired
    private ISysFileService sysFileService;

    /**
     * 查询文件列表
     */
    @PreAuthorize("@ss.hasPermi('filemanager:file:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysFile sysFile)
    {
        startPage();
        List<SysFile> list = sysFileService.selectSysFileList(sysFile);
        return getDataTable(list);
    }



    /**
     * 获取文件详细信息
     */
    @PreAuthorize("@ss.hasPermi('filemanager:file:query')")
    @GetMapping(value = "/{fileId}")
    public AjaxResult getInfo(@PathVariable("fileId") Long fileId)
    {
        return success(sysFileService.selectSysFileById(fileId));
    }

    /**
     * 新增文件
     */
    @PreAuthorize("@ss.hasPermi('filemanager:file:add')")
    @Log(title = "文件管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysFile sysFile)
    {
        return toAjax(sysFileService.insertSysFile(sysFile));
    }

    /**
     * 修改文件
     */
    @PreAuthorize("@ss.hasPermi('filemanager:file:edit')")
    @Log(title = "文件管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysFile sysFile)
    {
        return toAjax(sysFileService.updateSysFile(sysFile));
    }

    /**
     * 删除文件
     */
    @PreAuthorize("@ss.hasPermi('filemanager:file:remove')")
    @Log(title = "文件管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{fileIds}")
    public AjaxResult remove(@PathVariable Long[] fileIds)
    {
        return toAjax(sysFileService.deleteSysFileByIds(fileIds));
    }
    
    /**
     * 文件上传
     */
    @PreAuthorize("@ss.hasPermi('filemanager:file:upload')")
    @Log(title = "文件上传", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(MultipartFile file)
    {
        return sysFileService.uploadFile(file);
    }
    
    /**
     * 文件下载
     */
    @PreAuthorize("@ss.hasPermi('filemanager:file:download')")
    @Log(title = "文件下载", businessType = BusinessType.EXPORT)
    @GetMapping("/download/{fileId}")
    public void download(@PathVariable Long fileId)
    {
        sysFileService.downloadFile(fileId);
    }
    
    /**
     * 批量文件下载
     */
    @PreAuthorize("@ss.hasPermi('filemanager:file:download')")
    @Log(title = "批量文件下载", businessType = BusinessType.EXPORT)
    @GetMapping("/batchDownload/{fileIds}")
    public void batchDownload(@PathVariable Long[] fileIds)
    {
        sysFileService.batchDownloadFile(fileIds);
    }
} 