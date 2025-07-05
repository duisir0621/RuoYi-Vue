package com.ruoyi.common.utils.file;

/**
 * 媒体类型工具类
 * 
 * @author ruoyi
 */
public class MimeTypeUtils
{
    public static final String IMAGE_PNG = "image/png";

    public static final String IMAGE_JPG = "image/jpg";

    public static final String IMAGE_JPEG = "image/jpeg";

    public static final String IMAGE_BMP = "image/bmp";

    public static final String IMAGE_GIF = "image/gif";
    
    public static final String PDF = "application/pdf";
    
    public static final String[] IMAGE_EXTENSION = { "bmp", "gif", "jpg", "jpeg", "png" };

    public static final String[] FLASH_EXTENSION = { "swf", "flv" };

    public static final String[] MEDIA_EXTENSION = { "swf", "flv", "mp3", "wav", "wma", "wmv", "mid", "avi", "mpg",
            "asf", "rm", "rmvb", "mp4", "m4a", "m4v", "mov", "mkv", "webm", "ogg" };

    public static final String[] VIDEO_EXTENSION = { "mp4", "avi", "rmvb", "mov", "wmv", "flv", "mkv", "m4v", "webm" };

    public static final String[] DEFAULT_ALLOWED_EXTENSION = {
            // 图片
            "bmp", "gif", "jpg", "jpeg", "png",
            // word excel powerpoint
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "html", "htm", "txt",
            // 压缩文件
            "rar", "zip", "gz", "bz2",
            // 视频格式
            "mp4", "avi", "rmvb", "mov", "wmv", "flv", "mkv", "m4v", "webm",
            // 音频格式
            "mp3", "wav", "wma", "ogg", "m4a", "mid", 
            // pdf
            "pdf" };

    public static String getExtension(String prefix)
    {
        switch (prefix)
        {
            case IMAGE_PNG:
                return "png";
            case IMAGE_JPG:
                return "jpg";
            case IMAGE_JPEG:
                return "jpeg";
            case IMAGE_BMP:
                return "bmp";
            case IMAGE_GIF:
                return "gif";
            case PDF:
                return "pdf";
            default:
                return "";
        }
    }
    
    /**
     * 根据文件扩展名获取对应的MIME类型
     * 
     * @param extension 文件扩展名
     * @return MIME类型
     */
    public static String getContentType(String extension)
    {
        if (extension == null)
        {
            return "application/octet-stream";
        }
        
        extension = extension.toLowerCase().trim();
        
        switch (extension)
        {
            case "png":
                return IMAGE_PNG;
            case "jpg":
                return IMAGE_JPG;
            case "jpeg":
                return IMAGE_JPEG;
            case "bmp":
                return IMAGE_BMP;
            case "gif":
                return IMAGE_GIF;
            case "pdf":
                return PDF;
            case "doc":
            case "docx":
                return "application/msword";
            case "xls":
            case "xlsx":
                return "application/vnd.ms-excel";
            case "ppt":
            case "pptx":
                return "application/vnd.ms-powerpoint";
            case "html":
            case "htm":
                return "text/html";
            case "txt":
                return "text/plain";
            case "zip":
                return "application/zip";
            case "rar":
                return "application/x-rar-compressed";
            case "gz":
                return "application/gzip";
            case "mp4":
                return "video/mp4";
            case "m4v":
                return "video/x-m4v";
            case "m4a":
                return "audio/mp4";
            case "mov":
                return "video/quicktime";
            case "wmv":
                return "video/x-ms-wmv";
            case "flv":
                return "video/x-flv";
            case "mkv":
                return "video/x-matroska";
            case "webm":
                return "video/webm";
            case "avi":
                return "video/x-msvideo";
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "wma":
                return "audio/x-ms-wma";
            case "ogg":
                return "audio/ogg";
            case "mid":
            case "midi":
                return "audio/midi";
            default:
                return "application/octet-stream";
        }
    }
}
