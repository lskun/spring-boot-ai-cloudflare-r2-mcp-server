package com.lskun.mcp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class R2ServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(R2ServiceClient.class);
    private final S3Client s3Client;

    /**
     * Initializes the Cloudflare R2 client with the given credentials and endpoint.
     */
    public R2ServiceClient(
            @Value("${r2.access-key-id}") String accessKeyId,
            @Value("${r2.secret-access-key}") String secretAccessKey,
            @Value("${r2.endpoint}") String endpoint) {
        logger.info("Initializing R2ServiceClient with endpoint: {}", endpoint);
        
        // 创建HTTP客户端，设置更长的超时时间
        SdkHttpClient httpClient = ApacheHttpClient.builder()
                .connectionTimeout(Duration.ofSeconds(30))
                .socketTimeout(Duration.ofSeconds(120))
                .build();
        
        // 创建最简单的S3客户端配置
        this.s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .checksumValidationEnabled(false)
                        .pathStyleAccessEnabled(true)
                        .build())
                .httpClient(httpClient)
                .build();
                
        logger.info("R2ServiceClient initialized successfully");
    }

    /**
     * Lists all buckets in Cloudflare R2.
     */
    @Tool(description = """
    List all buckets in Cloudflare R2.
    """)
    public List<String> listBuckets() {
        logger.info("Fetching list of buckets.");
        try {
            List<String> bucketNames = new ArrayList<>();
            ListBucketsResponse response = s3Client.listBuckets();
            for (Bucket bucket : response.buckets()) {
                bucketNames.add(bucket.name());
            }
            logger.info("Buckets found: {}", bucketNames);
            return bucketNames;
        } catch (S3Exception e) {
            logger.error("Failed to list buckets: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to list buckets: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a new bucket in Cloudflare R2.
     */
    @Tool(description = """
    Create a new bucket in Cloudflare R2.
    """)
    public String createBucket(String bucketName) {
        logger.info("Creating bucket: {}", bucketName);
        try {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
            logger.info("Bucket '{}' created successfully.", bucketName);
            return "Bucket '" + bucketName + "' created successfully.";
        } catch (S3Exception e) {
            logger.error("Failed to create bucket '{}': {}", bucketName, e.getMessage(), e);
            throw new RuntimeException("Failed to create bucket '" + bucketName + "': " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a bucket from Cloudflare R2.
     */
    @Tool(description = """
    Delete a bucket from Cloudflare R2.
    """)
    public String deleteBucket(String bucketName) {
        logger.info("Deleting bucket: {}", bucketName);
        try {
            s3Client.deleteBucket(DeleteBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
            logger.info("Bucket '{}' deleted successfully.", bucketName);
            return "Bucket '" + bucketName + "' deleted successfully.";
        } catch (S3Exception e) {
            logger.error("Failed to delete bucket '{}': {}", bucketName, e.getMessage(), e);
            throw new RuntimeException("Failed to delete bucket '" + bucketName + "': " + e.getMessage(), e);
        }
    }

    /**
     * Lists objects in a bucket.
     */
    @Tool(description = """
    List objects in a bucket.
    """)
    public List<Map<String, String>> listObjects(String bucketName, String prefix) {
        logger.info("Listing objects in bucket: {} with prefix: {}", bucketName, prefix);
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();
            
            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            List<Map<String, String>> objects = new ArrayList<>();
            
            for (S3Object s3Object : response.contents()) {
                Map<String, String> objectInfo = new HashMap<>();
                objectInfo.put("key", s3Object.key());
                objectInfo.put("size", String.valueOf(s3Object.size()));
                objectInfo.put("lastModified", s3Object.lastModified().toString());
                objects.add(objectInfo);
            }
            
            logger.info("Found {} objects in bucket: {}", objects.size(), bucketName);
            return objects;
        } catch (S3Exception e) {
            logger.error("Failed to list objects in bucket '{}' with prefix '{}': {}", bucketName, prefix, e.getMessage(), e);
            throw new RuntimeException("Failed to list objects in bucket '" + bucketName + "' with prefix '" + prefix + "': " + e.getMessage(), e);
        }
    }

    /**
     * 上传对象到存储桶。支持所有文件类型，包括：
     * 文本文件（txt、xml、html、csv、md、json等）和
     * 二进制文件（pdf、doc、docx、xls、xlsx、图片、视频等）。
     */
    @Tool(description = """
    Upload an object to a bucket. Supports ALL file types including:
    TEXT files (txt, xml, html, csv, md, json, etc.) and
    BINARY files (pdf, doc, docx, xls, xlsx, images, videos, etc.).
    
    Parameters:
    - bucketName (string) - name of the bucket
    - key (string) - object key/filename with extension (e.g., "folder/file.txt")
    - content (string) - file content in one of three formats:
      * Text content: raw text data
      * Base64: Base64 encoded binary data
      * Path: local file system path to the file
    - contentType (string) - MIME type of the content (optional, will be inferred from file extension if not provided)
    - contentFormat (string) - format of the content: "text" (default), "base64", or "path"
    
    Common MIME types by category:
    Text files:
    - text/plain (txt)
    - text/html (html)
    - text/xml (xml)
    - text/csv (csv)
    - text/markdown (md)
    - application/json (json)
    
    Documents:
    - application/pdf (pdf)
    - application/msword (doc)
    - application/vnd.openxmlformats-officedocument.wordprocessingml.document (docx)
    - application/vnd.ms-excel (xls)
    - application/vnd.openxmlformats-officedocument.spreadsheetml.sheet (xlsx)
    
    Images:
    - image/jpeg (jpg, jpeg)
    - image/png (png)
    - image/gif (gif)
    - image/webp (webp)
    - image/svg+xml (svg)
    
    Audio/Video:
    - audio/mpeg (mp3)
    - audio/wav (wav)
    - video/mp4 (mp4)
    - video/webm (webm)
    
    Archives:
    - application/zip (zip)
    - application/x-7z-compressed (7z)
    - application/gzip (gz)
    
    Returns: Success message with object key if upload is successful
    Throws: 
    - IllegalArgumentException: If content format is invalid or file not found
    - S3Exception: If upload fails due to S3 service issues
    """)
    public String uploadObject(String bucketName, String key, String content, String contentType, String contentFormat) {
        logger.info("Uploading object to bucket: {} with key: {}, contentFormat: {}", bucketName, key, contentFormat);
        
        // 参数验证
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("Bucket name cannot be null or empty");
        }
        
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        
        // 设置默认内容格式为文本
        String format = contentFormat;
        if (format == null || format.trim().isEmpty()) {
            format = "text";
        }
        
        try {
            RequestBody requestBody;
            
            switch (format.toLowerCase()) {
                case "text":
                    // 直接使用文本内容
                    logger.info("Processing as text content, size: {} characters", content.length());
                    requestBody = RequestBody.fromString(content);
                    break;
                    
                case "base64":
                    // 解码Base64数据
                    try {
                        byte[] binaryData = java.util.Base64.getDecoder().decode(content);
                        logger.info("Decoded Base64 content, size: {} bytes", binaryData.length);
                        requestBody = RequestBody.fromBytes(binaryData);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid Base64 content: " + e.getMessage(), e);
                    }
                    break;
                    
                case "path":
                    // 从本地文件路径读取
                    try {
                        java.io.File file = new java.io.File(content);
                        if (!file.exists()) {
                            throw new IllegalArgumentException("File does not exist: " + content);
                        }
                        if (!file.isFile()) {
                            throw new IllegalArgumentException("Path is not a file: " + content);
                        }
                        
                        // 如果未提供contentType，从文件扩展名推断
                        String finalContentType = contentType;
                        if (finalContentType == null || finalContentType.trim().isEmpty()) {
                            finalContentType = inferContentTypeFromKey(file.getName());
                        }
                        
                        logger.info("Reading from file: {}, size: {} bytes", content, file.length());
                        requestBody = RequestBody.fromFile(file);
                        contentType = finalContentType;
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Failed to read file: " + content + " - " + e.getMessage(), e);
                    }
                    break;
                    
                default:
                    throw new IllegalArgumentException("Invalid content format: " + format + ". Must be 'text', 'base64', or 'path'.");
            }
            
            // 如果未提供contentType（且不是文件路径），尝试从key推断
            if ((contentType == null || contentType.trim().isEmpty()) && !"path".equalsIgnoreCase(format)) {
                contentType = inferContentTypeFromKey(key);
                logger.info("Content type inferred from key: {}", contentType);
            }
            
            // 上传对象
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();
            
            PutObjectResponse response = s3Client.putObject(putObjectRequest, requestBody);
            
            logger.info("Object uploaded successfully to bucket: '{}' with key: '{}', etag: '{}'", 
                    bucketName, key, response.eTag());
            
            return "Object uploaded successfully to bucket: '" + bucketName + "' with key: '" + key + "'.";
        } catch (S3Exception e) {
            logger.error("Failed to upload object to bucket '{}' with key '{}': {}", bucketName, key, e.getMessage(), e);
            throw new RuntimeException("Failed to upload object to bucket '" + bucketName + "' with key '" + key + "': " + e.getMessage(), e);
        }
    }

    /**
     * 统一的对象下载方法，根据内容类型和参数自动处理文本或二进制数据。
     * 不会返回byte[]类型，避免Cursor处理二进制数据的问题。
     */
    @Tool(description = """
    Download an object from a bucket.
    Supports ALL file types including TEXT files (txt, xml, html, csv, md, json, etc.)
    and BINARY files (pdf, doc, docx, images, videos, etc.).
    Parameters:
    - bucketName (string) - name of the bucket;
    - key (string) - object key/filename to download;
    - destinationPath (string, optional) - local path where the file should be saved (if not provided, file will be saved in the current project root directory using the original key as filename);
    - responseType (string, optional) - force specific handling: 'text' to return content as text, 'file' to save to file.
    Returns: For text files without destinationPath - the file content as String;
    For binary files or when destinationPath is provided - the path to the saved file.
    """)
    public String downloadObject(String bucketName, String key, String destinationPath, String responseType) {
        logger.info("Downloading object from bucket: {} with key: {}, destinationPath: {}, responseType: {}", 
                bucketName, key, destinationPath, responseType);
        
        // 参数验证
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("Bucket name cannot be null or empty");
        }
        
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        
        try {
            // 构建获取对象请求
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            // 尝试获取对象元数据
            HeadObjectResponse metadata;
            try {
                metadata = s3Client.headObject(HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build());
            } catch (S3Exception e) {
                logger.error("Failed to get object metadata: {}", e.getMessage());
                throw new RuntimeException("Object does not exist or cannot be accessed in bucket '" + bucketName + 
                        "' with key '" + key + "': " + e.getMessage(), e);
            }
            
            String contentType = metadata.contentType();
            long contentLength = metadata.contentLength();
            
            logger.info("Object content type: {}, content length: {} bytes", contentType, contentLength);
            
            // 确定处理模式：文本模式或文件模式
            boolean textMode;
            if (responseType != null && !responseType.isEmpty()) {
                // 显式指定了响应类型
                textMode = "text".equalsIgnoreCase(responseType);
                logger.info("Using forced response type: {}", textMode ? "text" : "file");
            } else {
                // 根据内容类型和destinationPath自动决定
                textMode = isTextContentType(contentType) && (destinationPath == null || destinationPath.trim().isEmpty());
                logger.info("Auto-detected response mode: {}", textMode ? "text" : "file");
            }
            
            if (textMode) {
                // 文本模式：直接返回内容
                ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
                try {
                    String textContent = new String(response.readAllBytes());
                    logger.info("Object downloaded as text, size: {} characters", textContent.length());
                    return textContent;
                } catch (IOException e) {
                    logger.error("Error reading object content: {}", e.getMessage(), e);
                    throw new RuntimeException("Error reading object from bucket '" + bucketName + 
                            "' with key '" + key + "': " + e.getMessage(), e);
                }
            } else {
                // 文件模式：保存到文件并返回文件路径
                String finalPath = destinationPath;
                
                // 如果未指定目标路径，创建临时文件
                if (finalPath == null || finalPath.trim().isEmpty()) {
                    String extension = "";
                    int lastDot = key.lastIndexOf('.');
                    if (lastDot > 0) {
                        extension = key.substring(lastDot);
                    }
                    
                    // 创建临时文件
                    try {
                        File tempFile = File.createTempFile("r2download_", extension);
                        finalPath = tempFile.getAbsolutePath();
                        logger.info("Created temporary file for download: {}", finalPath);
                    } catch (IOException e) {
                        logger.error("Failed to create temporary file: {}", e.getMessage(), e);
                        throw new RuntimeException("Failed to create temporary file for download: " + e.getMessage(), e);
                    }
                }
                
                // 确保目标目录存在
                File destinationFile = new File(finalPath);
                File parentDir = destinationFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        logger.warn("Failed to create directory: {}", parentDir.getAbsolutePath());
                    }
                }
                
                // 下载到文件
                s3Client.getObject(request, software.amazon.awssdk.core.sync.ResponseTransformer.toFile(destinationFile));
                
                logger.info("Object downloaded successfully to file: {}", finalPath);
                return "Object from bucket '" + bucketName + "' with key '" + key + 
                       "' downloaded to file: '" + finalPath + "'";
            }
        } catch (S3Exception e) {
            logger.error("Failed to download object from bucket '{}' with key '{}': {}", 
                    bucketName, key, e.getMessage(), e);
            throw new RuntimeException("Failed to download object from bucket '" + bucketName + 
                    "' with key '" + key + "': " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to determine if a content type is a text type
     */
    private boolean isTextContentType(String contentType) {
        if (contentType == null) {
            return false; // Assume binary if unknown
        }
        
        contentType = contentType.toLowerCase();
        
        // Common text content types
        return contentType.startsWith("text/") || 
               contentType.equals("application/json") ||
               contentType.equals("application/xml") ||
               contentType.equals("application/javascript") ||
               contentType.equals("application/typescript") ||
               contentType.equals("application/xhtml+xml") ||
               contentType.equals("application/x-www-form-urlencoded") ||
               contentType.contains("+json") ||
               contentType.contains("+xml");
    }

    /**
     * Deletes an object from a bucket.
     */
    @Tool(description = """
    Delete an object from a bucket.
    """)
    public String deleteObject(String bucketName, String key) {
        logger.info("Deleting object from bucket: {} with key: {}", bucketName, key);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            
            logger.info("Object deleted successfully from bucket: {} with key: {}", bucketName, key);
            return "Object deleted successfully from bucket: '" + bucketName + "' with key: '" + key + "'.";
        } catch (S3Exception e) {
            logger.error("Failed to delete object from bucket '{}' with key '{}': {}", bucketName, key, e.getMessage(), e);
            throw new RuntimeException("Failed to delete object from bucket '" + bucketName + "' with key '" + key + "': " + e.getMessage(), e);
        }
    }

    /**
     * Gets object metadata.
     */
    @Tool(description = """
    Get object metadata.
    """)
    public Map<String, String> getObjectMetadata(String bucketName, String key) {
        logger.info("Getting metadata for object in bucket: {} with key: {}", bucketName, key);
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            
            Map<String, String> metadata = new HashMap<>();
            metadata.put("contentType", response.contentType());
            metadata.put("contentLength", String.valueOf(response.contentLength()));
            metadata.put("lastModified", response.lastModified().toString());
            response.metadata().forEach(metadata::put);
            
            logger.info("Metadata retrieved successfully for object in bucket: {} with key: {}", bucketName, key);
            return metadata;
        } catch (S3Exception e) {
            logger.error("Failed to get metadata for object in bucket '{}' with key '{}': {}", bucketName, key, e.getMessage(), e);
            throw new RuntimeException("Failed to get metadata for object in bucket '" + bucketName + "' with key '" + key + "': " + e.getMessage(), e);
        }
    }

    /**
     * 根据文件扩展名推断contentType
     */
    private String inferContentTypeFromKey(String key) {
        String extension = "";
        int lastDot = key.lastIndexOf('.');
        if (lastDot > 0) {
            extension = key.substring(lastDot + 1).toLowerCase();
        }
        
        // 根据文件扩展名映射到MIME类型
        Map<String, String> extensionToMimeType = new HashMap<>();
        
        // 文本文件
        extensionToMimeType.put("txt", "text/plain");
        extensionToMimeType.put("html", "text/html");
        extensionToMimeType.put("htm", "text/html");
        extensionToMimeType.put("xml", "text/xml");
        extensionToMimeType.put("csv", "text/csv");
        extensionToMimeType.put("md", "text/markdown");
        extensionToMimeType.put("markdown", "text/markdown");
        extensionToMimeType.put("json", "application/json");
        extensionToMimeType.put("js", "application/javascript");
        extensionToMimeType.put("ts", "application/typescript");
        extensionToMimeType.put("css", "text/css");
        extensionToMimeType.put("rtf", "application/rtf");
        extensionToMimeType.put("yaml", "text/yaml");
        extensionToMimeType.put("yml", "text/yaml");
        
        // 二进制文档文件
        extensionToMimeType.put("pdf", "application/pdf");
        extensionToMimeType.put("doc", "application/msword");
        extensionToMimeType.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        extensionToMimeType.put("xls", "application/vnd.ms-excel");
        extensionToMimeType.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        extensionToMimeType.put("ppt", "application/vnd.ms-powerpoint");
        extensionToMimeType.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        extensionToMimeType.put("odt", "application/vnd.oasis.opendocument.text");
        extensionToMimeType.put("ods", "application/vnd.oasis.opendocument.spreadsheet");
        extensionToMimeType.put("odp", "application/vnd.oasis.opendocument.presentation");
        
        // 图片文件
        extensionToMimeType.put("jpg", "image/jpeg");
        extensionToMimeType.put("jpeg", "image/jpeg");
        extensionToMimeType.put("png", "image/png");
        extensionToMimeType.put("gif", "image/gif");
        extensionToMimeType.put("svg", "image/svg+xml");
        extensionToMimeType.put("webp", "image/webp");
        extensionToMimeType.put("ico", "image/x-icon");
        extensionToMimeType.put("bmp", "image/bmp");
        extensionToMimeType.put("tiff", "image/tiff");
        extensionToMimeType.put("tif", "image/tiff");
        
        // 音频文件
        extensionToMimeType.put("mp3", "audio/mpeg");
        extensionToMimeType.put("wav", "audio/wav");
        extensionToMimeType.put("ogg", "audio/ogg");
        extensionToMimeType.put("m4a", "audio/mp4");
        extensionToMimeType.put("flac", "audio/flac");
        extensionToMimeType.put("aac", "audio/aac");
        
        // 视频文件
        extensionToMimeType.put("mp4", "video/mp4");
        extensionToMimeType.put("avi", "video/x-msvideo");
        extensionToMimeType.put("webm", "video/webm");
        extensionToMimeType.put("mkv", "video/x-matroska");
        extensionToMimeType.put("mov", "video/quicktime");
        extensionToMimeType.put("wmv", "video/x-ms-wmv");
        extensionToMimeType.put("flv", "video/x-flv");
        
        // 压缩文件
        extensionToMimeType.put("zip", "application/zip");
        extensionToMimeType.put("rar", "application/vnd.rar");
        extensionToMimeType.put("7z", "application/x-7z-compressed");
        extensionToMimeType.put("tar", "application/x-tar");
        extensionToMimeType.put("gz", "application/gzip");
        
        // 字体文件
        extensionToMimeType.put("ttf", "font/ttf");
        extensionToMimeType.put("otf", "font/otf");
        extensionToMimeType.put("woff", "font/woff");
        extensionToMimeType.put("woff2", "font/woff2");
        
        // 代码和编程文件
        extensionToMimeType.put("java", "text/x-java-source");
        extensionToMimeType.put("py", "text/x-python");
        extensionToMimeType.put("cpp", "text/x-c++src");
        extensionToMimeType.put("c", "text/x-csrc");
        extensionToMimeType.put("cs", "text/x-csharp");
        extensionToMimeType.put("php", "application/x-php");
        extensionToMimeType.put("rb", "text/x-ruby");
        extensionToMimeType.put("go", "text/x-go");
        extensionToMimeType.put("swift", "text/x-swift");
        
        // 其他常见文件类型
        extensionToMimeType.put("exe", "application/x-msdownload");
        extensionToMimeType.put("bin", "application/octet-stream");
        extensionToMimeType.put("dll", "application/x-msdownload");
        extensionToMimeType.put("iso", "application/x-iso9660-image");
        extensionToMimeType.put("apk", "application/vnd.android.package-archive");
        extensionToMimeType.put("dmg", "application/x-apple-diskimage");
        
        logger.debug("Inferring content type for extension: {}", extension);
        
        // 返回匹配的MIME类型，如果没有匹配项则返回通用的二进制流类型
        String contentType = extensionToMimeType.getOrDefault(extension, "application/octet-stream");
        logger.debug("Inferred content type: {}", contentType);
        
        return contentType;
    }
}