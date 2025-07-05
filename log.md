# 项目变更日志

## 2025-07-10 更新

1. 更新了项目分析报告，重点优化了文件管理模块部分：
   - 添加了更多文件类型支持，包括各种视频和音频格式
   - 优化了文件下载功能，特别是PDF文件的处理
   - 增强了文件预览功能和安全处理
   - 改进了批量下载功能，解决了文件重名问题
   - 完善了MIME类型支持

## 2025-07-11 更新

1. 修改了文件上传大小限制：
   - 将单个文件大小限制从10MB增加到100MB
   - 将总请求大小限制从20MB增加到200MB
   - 解决了"MaxUploadSizeExceededException"错误
   - 优化了大文件上传体验 

# 系统更新日志

## 2025-07-05 文件管理批量删除功能优化

### 问题描述
文件管理中，批量删除全部文件时，PDF文件和压缩包（如ZIP）实际没有从服务器存储中删除，导致这些文件在后台仍然存在。

### 解决方案
1. 增强 `FileUtils.deleteFile` 方法，针对PDF和压缩包等特殊文件类型的删除处理：
   - 添加多次尝试删除机制
   - 使用 `System.gc()` 触发垃圾回收，释放文件占用
   - 使用Java NIO的 `Files.delete()` 方法作为备用方案
   - 添加详细的日志记录功能

2. 新增 `FileCleanupUtils` 工具类，提供延迟文件清理功能：
   - 通过定时任务机制，定期尝试删除之前未能删除的文件
   - 设置最大尝试次数，避免无限尝试
   - 采用线程安全的 `ConcurrentHashMap` 存储待删除文件
   - 优雅关闭调度器，确保应用程序退出时资源释放

3. 优化 `SysFileServiceImpl` 文件删除逻辑：
   - 使用 slf4j 替代 System.out/err，提供更标准的日志输出
   - 集成 `FileCleanupUtils` 处理删除失败的文件
   - 使用 `deleteOnExit()` 作为双重保险，确保JVM退出时删除文件
   - 增加详细的删除成功/失败统计与日志

4. 增加 `FileManagerConfig` 配置类：
   - 在应用启动时初始化文件清理任务
   - 添加JVM关闭钩子，确保应用退出时调度器正确关闭

### 修改文件
1. ruoyi-common/src/main/java/com/ruoyi/common/utils/file/FileUtils.java
2. ruoyi-filemanager/src/main/java/com/ruoyi/filemanager/service/impl/SysFileServiceImpl.java
3. ruoyi-filemanager/src/main/java/com/ruoyi/filemanager/utils/FileCleanupUtils.java (新增)
4. ruoyi-filemanager/src/main/java/com/ruoyi/filemanager/config/FileManagerConfig.java (新增)

### 技术要点
- 使用Java NIO API进行文件操作
- 利用定时调度处理异步文件清理
- 采用多种删除策略确保文件删除成功率
- 应用启动/关闭生命周期管理
- 完善的日志记录机制 