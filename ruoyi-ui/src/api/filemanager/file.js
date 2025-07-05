import request from '@/utils/request'

// 查询文件列表
export function listFile(query) {
  return request({
    url: '/filemanager/file/list',
    method: 'get',
    params: query
  })
}

// 查询文件详细
export function getFile(fileId) {
  return request({
    url: '/filemanager/file/' + fileId,
    method: 'get'
  })
}

// 新增文件
export function addFile(data) {
  return request({
    url: '/filemanager/file',
    method: 'post',
    data: data
  })
}

// 修改文件
export function updateFile(data) {
  return request({
    url: '/filemanager/file',
    method: 'put',
    data: data
  })
}

// 删除文件
export function delFile(fileId) {
  return request({
    url: '/filemanager/file/' + fileId,
    method: 'delete'
  })
}

// 导出文件
export function exportFile(query) {
  return request({
    url: '/filemanager/file/export',
    method: 'post',
    params: query
  })
}

// 上传文件
export function uploadFile(file) {
  return request({
    url: '/filemanager/file/upload',
    method: 'post',
    data: file
  })
}

// 下载文件
export function downloadFile(fileId) {
  return request({
    url: '/filemanager/file/download/' + fileId,
    method: 'get',
    responseType: 'blob'
  })
}

// 批量下载文件
export function batchDownloadFile(fileIds) {
  return request({
    url: '/filemanager/file/batchDownload/' + fileIds,
    method: 'get',
    responseType: 'blob'
  })
} 