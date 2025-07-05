-- 创建文件表
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
  `file_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `file_name` varchar(100) NOT NULL COMMENT '文件名称',
  `original_name` varchar(100) NOT NULL COMMENT '原始文件名',
  `file_path` varchar(255) NOT NULL COMMENT '文件路径',
  `file_type` varchar(100) NOT NULL COMMENT '文件类型',
  `file_size` bigint(20) NOT NULL COMMENT '文件大小(KB)',
  `status` char(1) DEFAULT '0' COMMENT '文件状态（0正常 1删除）',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `download_count` bigint(20) DEFAULT '0' COMMENT '下载次数',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`file_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8 COMMENT='文件管理表';

-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('文件管理', '3', '10', 'file', 'filemanager/file/index', 1, 0, 'C', '0', '0', 'filemanager:file:list', 'file', 'admin', sysdate(), '', null, '文件管理菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('文件管理查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'filemanager:file:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('文件管理新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'filemanager:file:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('文件管理修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'filemanager:file:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('文件管理删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'filemanager:file:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('文件管理导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'filemanager:file:export',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('文件上传', @parentId, '6',  '#', '', 1, 0, 'F', '0', '0', 'filemanager:file:upload',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('文件下载', @parentId, '7',  '#', '', 1, 0, 'F', '0', '0', 'filemanager:file:download',       '#', 'admin', sysdate(), '', null, ''); 