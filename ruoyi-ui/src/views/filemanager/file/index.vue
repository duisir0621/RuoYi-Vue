<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="文件名称" prop="fileName">
        <el-input
          v-model="queryParams.fileName"
          placeholder="请输入文件名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="原始名称" prop="originalName">
        <el-input
          v-model="queryParams.originalName"
          placeholder="请输入原始文件名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="文件类型" prop="fileType">
        <el-input
          v-model="queryParams.fileType"
          placeholder="请输入文件类型"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="文件状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="文件状态" clearable>
          <el-option
            v-for="dict in dict.type.sys_normal_disable"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间">
        <el-date-picker
          v-model="dateRange"
          style="width: 240px"
          value-format="yyyy-MM-dd"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleUpload"
          v-hasPermi="['filemanager:file:upload']"
        >上传</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['filemanager:file:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          :disabled="multiple"
          @click="handleBatchDownload"
          v-hasPermi="['filemanager:file:download']"
        >批量下载</el-button>
      </el-col>

      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="fileList" ref="multipleTable" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="文件ID" align="center" prop="fileId" />
      <el-table-column label="文件名称" align="center" prop="fileName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="原始文件名" align="center" prop="originalName" width="220" :show-overflow-tooltip="true" />
      <el-table-column label="缩略图" align="center" width="80">
        <template slot-scope="scope">
          <div v-if="isImageFile(scope.row.fileType)" class="thumbnail-container" @click="handleImagePreview(scope.row)">
            <el-image 
              :src="getFilePath(scope.row.filePath)" 
              fit="cover"
              class="thumbnail-image">
              <div slot="error" class="image-error">
                <i class="el-icon-picture-outline"></i>
              </div>
            </el-image>
          </div>
          <div v-else class="file-type-icon">
            <i :class="getFileIcon(scope.row.fileType)"></i>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="文件类型" align="center" prop="fileType" width="100" />
      <el-table-column label="文件大小(KB)" align="center" prop="fileSize" width="100" />
      <el-table-column label="文件状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="下载次数" align="center" prop="downloadCount" width="90" />
      <el-table-column label="访问地址" align="center" width="120">
        <template slot-scope="scope">
          <el-link 
            type="primary" 
            icon="el-icon-link" 
            @click.native="copyFileUrl(scope.row)">
            点击复制
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="240">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row)"
            v-hasPermi="['filemanager:file:query']"
          >预览</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['filemanager:file:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['filemanager:file:remove']"
          >删除</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-download"
            @click="handleDownload(scope.row)"
            v-hasPermi="['filemanager:file:download']"
          >下载</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改文件对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="文件名称" prop="fileName">
          <el-input v-model="form.fileName" placeholder="请输入文件名称" />
        </el-form-item>
        <el-form-item label="文件状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.sys_normal_disable"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
    
    <!-- 文件上传对话框 -->
    <el-dialog :title="upload.title" :visible.sync="upload.open" width="500px" append-to-body>
      <el-upload
        class="upload-file"
        ref="upload"
        :limit="1"
        accept=".jpg, .jpeg, .png, .gif, .bmp, .pdf, .doc, .docx, .xls, .xlsx, .ppt, .pptx, .txt, .zip, .rar, .mp4, .mp3, .avi, .rmvb, .mov, .wmv, .flv, .mkv, .m4v, .webm, .wav, .wma, .ogg, .m4a, .mid"
        :action="upload.url"
        :headers="upload.headers"
        :file-list="upload.fileList"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :auto-upload="true"
        drag
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip text-center" slot="tip">
          <span>上传文件大小不超过 {{ upload.fileSize }}MB</span>
        </div>
      </el-upload>
    </el-dialog>
    
    <!-- 文件预览对话框 -->
    <el-dialog :title="preview.title" :visible.sync="preview.open" width="80%" append-to-body>
      <div class="preview-container" v-if="preview.type === 'image'">
        <el-image :src="preview.url" alt="预览图片" :preview-src-list="[preview.url]" fit="contain" class="preview-image"></el-image>
      </div>
      <div class="preview-container" v-else-if="preview.type === 'pdf'">
        <iframe :src="preview.url" width="100%" height="500px"></iframe>
      </div>
      <div class="preview-container" v-else>
        <p>该文件类型不支持预览，请下载后查看</p>
      </div>
    </el-dialog>

    <!-- 图片预览弹窗 -->
    <el-dialog :title="imagePreview.title" :visible.sync="imagePreview.open" append-to-body width="90%" class="image-preview-dialog" :fullscreen="true">
      <div class="fullscreen-preview-container">
        <el-image 
          :src="imagePreview.url" 
          :preview-src-list="[imagePreview.url]"
          fit="none"
          class="fullscreen-preview-image">
        </el-image>
      </div>
    </el-dialog>

    <!-- 隐藏的图片元素，用于触发原生预览 -->
    <div style="display: none;">
      <el-image 
        ref="previewImage"
        :src="imagePreview.url" 
        :preview-src-list="imagePreview.previewList">
      </el-image>
    </div>
  </div>
</template>

<script>
import { listFile, getFile, delFile, addFile, updateFile } from "@/api/filemanager/file";
import { getToken } from "@/utils/auth";

export default {
  name: "File",
  dicts: ['sys_normal_disable'],
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 文件表格数据
      fileList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 日期范围
      dateRange: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        fileName: null,
        originalName: null,
        fileType: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        fileName: [
          { required: true, message: "文件名称不能为空", trigger: "blur" }
        ],
        status: [
          { required: true, message: "文件状态不能为空", trigger: "change" }
        ]
      },
      // 上传参数
      upload: {
        // 是否显示弹出层
        open: false,
        // 弹出层标题
        title: "上传文件",
        // 是否禁用上传
        isUploading: false,
        // 设置上传的请求头部
        headers: { Authorization: "Bearer " + getToken() },
        // 上传的地址
        url: process.env.VUE_APP_BASE_API + "/filemanager/file/upload",
        // 上传的文件列表
        fileList: [],
        // 上传文件大小限制
        fileSize: 100
      },
      // 预览参数
      preview: {
        open: false,
        title: "文件预览",
        url: "",
        type: ""
      },
      // 图片预览参数
      imagePreview: {
        url: "",
        previewList: []
      },
      // 下载防抖
      isDownloading: false
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询文件列表 */
    getList() {
      this.loading = true;
      listFile(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.fileList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        fileId: null,
        fileName: null,
        originalName: null,
        filePath: null,
        fileType: null,
        fileSize: null,
        status: "0",
        deptId: null,
        downloadCount: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        remark: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.dateRange = [];
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.fileId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const fileId = row.fileId || this.ids;
      getFile(fileId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改文件信息";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.fileId != null) {
            updateFile(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addFile(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const fileIds = row.fileId || this.ids;
      this.$modal.confirm('是否确认删除文件编号为"' + fileIds + '"的数据项？').then(function() {
        return delFile(fileIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },

    /** 上传按钮操作 */
    handleUpload() {
      this.upload.open = true;
      this.upload.fileList = [];
    },
    // 文件上传成功处理
    handleUploadSuccess(response, file, fileList) {
      this.upload.open = false;
      this.upload.fileList = [];
      this.$modal.msgSuccess("上传成功");
      this.getList();
    },
    // 文件上传失败处理
    handleUploadError() {
      this.$modal.msgError("上传失败");
      this.upload.open = false;
    },
    /** 下载按钮操作 */
    handleDownload(row) {
      // 防止重复点击
      if (this.isDownloading) {
        return;
      }
      this.isDownloading = true;

      // 所有文件类型都直接下载，不再弹出选择框
      this.downloadFile(row, 'download');
    },

    /** 实际下载文件操作 */
    downloadFile(row, mode) {
      const fileId = row.fileId || this.ids;
      const fileType = row.fileType ? row.fileType.toLowerCase() : '';
      const isPdf = fileType === 'pdf';
      const originalName = row.originalName || `file_${fileId}`;
      
      // 构建请求URL
      const token = getToken();
      const url = process.env.VUE_APP_BASE_API + '/filemanager/file/download/' + fileId + '?token=' + token;
      
      // 如果选择在线预览PDF，使用浏览器的PDF查看器
      if (mode === 'preview' && isPdf) {
        // 打开新窗口预览PDF
        window.open(url, '_blank');
        this.isDownloading = false;
        return;
      }

      // console.log('开始下载文件 - URL:', url);
      
      try {
        // 使用iframe方式下载文件，避免被浏览器阻止
        // 创建一个隐藏的iframe
        let downloadFrame = document.getElementById('download-frame');
        if (!downloadFrame) {
          downloadFrame = document.createElement('iframe');
          downloadFrame.id = 'download-frame';
          downloadFrame.style.display = 'none';
          document.body.appendChild(downloadFrame);
        }
        
       // 设置下载完成或失败的处理函数
        const downloadTimeout = setTimeout(() => {
          //console.log('下载可能已开始或被阻止');
          // 只显示简短的成功提示，不再提示检查下载列表
          //this.$modal.msgSuccess("下载成功");
          this.isDownloading = false;
        }, 1500); // 缩短超时时间，提高响应速度
        
        // 监听iframe的load事件
        downloadFrame.onload = () => {
          clearTimeout(downloadTimeout);
          console.log('iframe加载完成');
          
          try {
            // 尝试检查iframe内容是否为错误响应
            const frameDoc = downloadFrame.contentDocument || downloadFrame.contentWindow.document;
            if (frameDoc && frameDoc.body && frameDoc.body.textContent) {
              const text = frameDoc.body.textContent;
              // 检查是否包含错误信息
              if (text.includes('error') || text.includes('错误') || text.includes('失败')) {
                this.$modal.msgError(`下载失败: ${text}`);
                this.isDownloading = false;
                return;
              }
            }
          } catch (e) {
            // 跨域限制可能导致无法访问iframe内容
            console.log('无法检查iframe内容:', e);
          }
          
          // 默认认为下载已开始，但不显示提示，保持界面简洁
          this.isDownloading = false;
        };
        
        // 监听iframe的错误事件
        downloadFrame.onerror = () => {
          clearTimeout(downloadTimeout);
          console.error('iframe加载错误');
          this.$modal.msgError("下载失败，请稍后重试");
          this.isDownloading = false;
        };
        
        // 对于PDF文件，添加特殊参数以确保正确下载
        let downloadUrl = url;
        if (isPdf) {
          // 添加参数指示这是下载而非预览
          downloadUrl += '&download=true';
          // 添加原始文件名作为参数
          if (originalName) {
            downloadUrl += `&fileName=${encodeURIComponent(originalName)}`;
          }
        }
        
        // 设置iframe的src以触发下载
        downloadFrame.src = downloadUrl;
        
      } catch (error) {
        console.error('下载文件时出错:', error);
        this.$modal.msgError(`下载失败: ${error.message}`);
        setTimeout(() => { this.isDownloading = false; }, 1000);
      }
    },
    /** 批量下载按钮操作 */
    handleBatchDownload() {
      const selection = this.$refs.multipleTable.selection;
      if (selection.length === 0) {
        this.$modal.msgError("请至少选择一个文件");
        return;
      }

      if (this.isDownloading) {
        this.$modal.msgWarning("正在处理另一个下载任务，请稍后再试");
        return;
      }

      this.isDownloading = true;
      
      // 获取所有选中文件的ID
      const fileIds = selection.map(item => item.fileId).join(",");
      
      // 构建批量下载URL
      const token = getToken();
      const url = process.env.VUE_APP_BASE_API + '/filemanager/file/batchDownload/' + fileIds + '?token=' + token;
      
      try {
        // 使用iframe方式下载文件，避免被浏览器阻止
        let downloadFrame = document.getElementById('download-frame');
        if (!downloadFrame) {
          downloadFrame = document.createElement('iframe');
          downloadFrame.id = 'download-frame';
          downloadFrame.style.display = 'none';
          document.body.appendChild(downloadFrame);
        }
        
        // 设置下载完成或失败的处理函数
        const downloadTimeout = setTimeout(() => {
          this.$modal.msgSuccess("批量下载已开始");
          this.isDownloading = false;
        }, 1500);
        
        // 监听iframe的load事件
        downloadFrame.onload = () => {
          clearTimeout(downloadTimeout);
          console.log('批量下载iframe加载完成');
          
          try {
            // 尝试检查iframe内容是否为错误响应
            const frameDoc = downloadFrame.contentDocument || downloadFrame.contentWindow.document;
            if (frameDoc && frameDoc.body && frameDoc.body.textContent) {
              const text = frameDoc.body.textContent;
              // 检查是否包含错误信息
              if (text.includes('error') || text.includes('错误') || text.includes('失败')) {
                this.$modal.msgError(`批量下载失败: ${text}`);
                this.isDownloading = false;
                return;
              }
            }
          } catch (e) {
            // 跨域限制可能导致无法访问iframe内容
            console.log('无法检查iframe内容:', e);
          }
          
          // 默认认为下载已开始
          this.isDownloading = false;
        };
        
        // 监听iframe的错误事件
        downloadFrame.onerror = () => {
          clearTimeout(downloadTimeout);
          console.error('批量下载iframe加载错误');
          this.$modal.msgError("批量下载失败，请稍后重试");
          this.isDownloading = false;
        };
        
        // 设置iframe的src以触发下载
        downloadFrame.src = url;
        
      } catch (error) {
        console.error('批量下载文件时出错:', error);
        this.$modal.msgError(`批量下载失败: ${error.message}`);
        setTimeout(() => { this.isDownloading = false; }, 1000);
      }
    },
    /** 预览按钮操作 */
    handleView(row) {
      const fileType = row.fileType.toLowerCase();
      const imageTypes = ['png', 'jpg', 'jpeg', 'gif', 'bmp'];
      
      // 构建预览URL
      const url = process.env.VUE_APP_BASE_API + row.filePath;
      
      if (imageTypes.includes(fileType)) {
        this.preview.type = 'image';
      } else if (fileType === 'pdf') {
        this.preview.type = 'pdf';
      } else {
        this.preview.type = 'other';
      }
      
      this.preview.title = "预览文件: " + row.originalName;
      this.preview.url = url;
      this.preview.open = true;
    },
    /** 判断是否为图片文件 */
    isImageFile(fileType) {
      if (!fileType) {
        return false;
      }
      const imageTypes = ['png', 'jpg', 'jpeg', 'gif', 'bmp'];
      return imageTypes.includes(fileType.toLowerCase());
    },
    /** 获取文件对应的图标 */
    getFileIcon(fileType) {
      if (!fileType) {
        return 'el-icon-document';
      }
      
      fileType = fileType.toLowerCase();
      
      if (['doc', 'docx'].includes(fileType)) {
        return 'el-icon-document';
      } else if (['xls', 'xlsx'].includes(fileType)) {
        return 'el-icon-tickets';
      } else if (['pdf'].includes(fileType)) {
        return 'el-icon-tickets';
      } else if (['zip', 'rar', '7z'].includes(fileType)) {
        return 'el-icon-folder';
      } else if (['txt'].includes(fileType)) {
        return 'el-icon-document-remove';
      } else {
        return 'el-icon-document';
      }
    },
    /** 获取文件路径 */
    getFilePath(path) {
      if (!path) {
        return '';
      }
      return process.env.VUE_APP_BASE_API + path;
    },
    /** 获取文件完整访问URL(包含当前服务器域名和端口) */
    getFileFullUrl(path) {
      if (!path) {
        return '';
      }
      // 获取当前页面的协议、主机名和端口
      const protocol = window.location.protocol;
      const hostname = window.location.hostname;
      const port = window.location.port ? `:${window.location.port}` : '';
      
      // 构建完整URL
      const baseUrl = `${protocol}//${hostname}${port}`;
      const apiPath = process.env.VUE_APP_BASE_API;
      
      // 如果path已经包含完整URL，则直接返回
      if (path.startsWith('http://') || path.startsWith('https://')) {
        return path;
      }
      
      return `${baseUrl}${apiPath}${path}`;
    },
    /** 复制文件访问地址 */
    copyFileUrl(row) {
      const url = this.getFileFullUrl(row.filePath);
      const input = document.createElement('input');
      input.value = url;
      document.body.appendChild(input);
      input.select();
      if (document.execCommand('copy')) {
        this.$modal.msgSuccess('复制成功');
      } else {
        this.$modal.msgError('复制失败');
      }
      document.body.removeChild(input);
    },
    /** 图片缩略图点击预览 */
    handleImagePreview(row) {
      // 构建预览URL
      const url = this.getFilePath(row.filePath);
      
      // 创建一个临时的Image对象预加载图片
      const img = new Image();
      img.onload = () => {
        // 设置预览列表
        this.imagePreview.url = url;
        this.imagePreview.previewList = [url];
        
        // 等待DOM更新后触发预览
        this.$nextTick(() => {
          // 找到预览图片的DOM元素并模拟点击
          const previewDom = this.$refs.previewImage;
          if (previewDom) {
            // 触发el-image组件的原生预览功能
            previewDom.clickHandler();
          }
        });
      };
      img.onerror = () => {
        this.$modal.msgError("图片加载失败");
      };
      img.src = url;
    }
  }
};
</script>

<style scoped>
.upload-file {
  text-align: center;
  margin: 0 auto;
}

.preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
  max-height: 80vh;
  overflow: auto;
}

.preview-image {
  max-width: 100%;
  object-fit: contain;
}

.thumbnail-container {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #ebeef5;
  margin: 0 auto;
  display: flex;
  justify-content: center;
  align-items: center;
}

.thumbnail-image {
  width: 100%;
  height: 100%;
}

.file-type-icon {
  width: 50px;
  height: 50px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0 auto;
}

.file-type-icon i {
  font-size: 24px;
  color: #909399;
}

.image-error {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background-color: #f5f7fa;
}

/* 不需要任何额外样式，使用Element UI原生的预览功能 */

/* 优化操作栏按钮的样式 */
.small-padding.fixed-width {
  min-width: 240px !important;
}

.small-padding.fixed-width .cell {
  padding: 0 5px;
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
}

.small-padding.fixed-width .cell .el-button {
  margin: 2px 3px;
  padding-left: 5px;
  padding-right: 5px;
}
</style> 