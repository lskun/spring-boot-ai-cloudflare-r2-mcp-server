package com.lskun.mcp.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R2ServiceClient测试类
 */
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class R2ServiceClientTest {

    @Autowired
    private R2ServiceClient r2ServiceClient;
    
    private String testBucketName;
    private final String testObjectKey = "test-object.txt";
    private final String testObjectContent = "这是一个测试内容";
    private final String testContentType = "text/plain";

    @BeforeEach
    public void setup() {
        // 生成唯一的测试桶名称
        testBucketName = "test-bucket-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @AfterEach
    public void cleanup() {
        try {
            // 尝试删除测试对象
            r2ServiceClient.deleteObject(testBucketName, testObjectKey);
        } catch (Exception e) {
            // 忽略删除对象时的错误
        }

        try {
            // 尝试删除测试桶
            r2ServiceClient.deleteBucket(testBucketName);
        } catch (Exception e) {
            // 忽略删除桶时的错误
        }
    }

    /**
     * 测试listBuckets方法
     * 该方法用于列出Cloudflare R2中的所有存储桶
     */
    @Test
    @Order(1)
    public void testListBuckets() {
        // 调用listBuckets方法
        List<String> buckets = r2ServiceClient.listBuckets();
        
        // 验证返回结果不为null
        assertNotNull(buckets, "返回的存储桶列表不应为null");
        
        // 打印结果
        System.out.println("找到的存储桶列表：" + buckets);
    }

    /**
     * 测试createBucket方法
     * 该方法用于在Cloudflare R2中创建新的存储桶
     */
    @Test
    @Order(2)
    public void testCreateBucket() {
        // 调用createBucket方法
        String result = r2ServiceClient.createBucket(testBucketName);
        
        // 验证返回结果
        assertNotNull(result, "创建桶的结果不应为null");
        assertTrue(result.contains("created successfully"), "创建桶应返回成功信息");
        
        // 验证桶确实被创建了
        List<String> buckets = r2ServiceClient.listBuckets();
        assertTrue(buckets.contains(testBucketName), "新创建的桶应该出现在桶列表中");
        
        System.out.println("创建桶的结果：" + result);
    }

    /**
     * 测试上传对象方法
     * 该方法用于向存储桶中上传对象
     */
    @Test
    @Order(3)
    public void testUploadObject() {
        // 首先创建一个桶
        r2ServiceClient.createBucket(testBucketName);
        
        // 调用uploadObject方法
        String result = r2ServiceClient.uploadObject(testBucketName, testObjectKey, testObjectContent, testContentType, "text");
        
        // 验证返回结果
        assertNotNull(result, "上传对象的结果不应为null");
        assertTrue(result.contains("uploaded successfully"), "上传对象应返回成功信息");
        
        System.out.println("上传对象的结果：" + result);
    }

    /**
     * 测试上传base64编码的内容
     */
    @Test
    @Order(3)
    public void testUploadBase64Content() {
        // 首先创建一个桶
        r2ServiceClient.createBucket(testBucketName);
        
        // Base64编码的简单文本
        String base64Content = java.util.Base64.getEncoder().encodeToString("这是Base64编码的测试内容".getBytes());
        
        // 调用uploadObject方法，使用base64格式
        String result = r2ServiceClient.uploadObject(testBucketName, "base64-test.bin", base64Content, "application/octet-stream", "base64");
        
        // 验证返回结果
        assertNotNull(result, "上传对象的结果不应为null");
        assertTrue(result.contains("uploaded successfully"), "上传对象应返回成功信息");
        
        // 下载并验证内容
        String downloadedContent = r2ServiceClient.downloadObject(testBucketName, "base64-test.bin", null, "text");
        assertEquals("这是Base64编码的测试内容", downloadedContent, "下载的内容应与上传前的原始内容一致");
        
        System.out.println("上传Base64内容的结果：" + result);
    }
    
    /**
     * 测试从文件路径上传内容
     */
    @Test
    @Order(3)
    public void testUploadFromPath() throws Exception {
        // 首先创建一个桶
        r2ServiceClient.createBucket(testBucketName);
        
        // 创建一个临时文件
        java.io.File tempFile = java.io.File.createTempFile("upload-test-", ".txt");
        tempFile.deleteOnExit();
        
        // 写入内容到临时文件
        try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
            writer.write("这是从文件上传的测试内容");
        }
        
        // 调用uploadObject方法，使用path格式
        String result = r2ServiceClient.uploadObject(testBucketName, "path-test.txt", tempFile.getAbsolutePath(), "text/plain", "path");
        
        // 验证返回结果
        assertNotNull(result, "上传对象的结果不应为null");
        assertTrue(result.contains("uploaded successfully"), "上传对象应返回成功信息");
        
        // 下载并验证内容
        String downloadedContent = r2ServiceClient.downloadObject(testBucketName, "path-test.txt", null, "text");
        assertEquals("这是从文件上传的测试内容", downloadedContent, "下载的内容应与上传前的原始内容一致");
        
        System.out.println("从文件路径上传内容的结果：" + result);
    }
    
    /**
     * 测试默认内容格式（text）
     */
    @Test
    @Order(3)
    public void testDefaultContentFormat() {
        // 首先创建一个桶
        r2ServiceClient.createBucket(testBucketName);
        
        // 调用uploadObject方法，不指定contentFormat
        String result = r2ServiceClient.uploadObject(testBucketName, "default-format.txt", "这是使用默认格式的测试内容", "text/plain", null);
        
        // 验证返回结果
        assertNotNull(result, "上传对象的结果不应为null");
        assertTrue(result.contains("uploaded successfully"), "上传对象应返回成功信息");
        
        // 下载并验证内容
        String downloadedContent = r2ServiceClient.downloadObject(testBucketName, "default-format.txt", null, "text");
        assertEquals("这是使用默认格式的测试内容", downloadedContent, "下载的内容应与上传的内容一致");
        
        System.out.println("使用默认内容格式上传的结果：" + result);
    }

    /**
     * 测试列出桶中对象的方法
     * 该方法用于列出存储桶中的所有对象
     */
    @Test
    @Order(4)
    public void testListObjects() {
        // 首先创建一个桶并上传一个对象
        r2ServiceClient.createBucket(testBucketName);
        r2ServiceClient.uploadObject(testBucketName, testObjectKey, testObjectContent, testContentType, "text");
        
        // 调用listObjects方法
        List<Map<String, String>> objects = r2ServiceClient.listObjects(testBucketName, "");
        
        // 验证返回结果
        assertNotNull(objects, "返回的对象列表不应为null");
        assertFalse(objects.isEmpty(), "对象列表不应为空");
        assertEquals(testObjectKey, objects.get(0).get("key"), "对象名称应匹配上传的名称");
        
        System.out.println("找到的对象列表：" + objects);
    }

    /**
     * 测试下载对象方法
     * 该方法用于从存储桶中下载对象
     */
    @Test
    @Order(5)
    public void testDownloadObject() {
        // 首先创建一个桶并上传一个对象
        r2ServiceClient.createBucket(testBucketName);
        r2ServiceClient.uploadObject(testBucketName, testObjectKey, testObjectContent, testContentType, "text");
        
        // 调用downloadObject方法并指定返回类型为text
        String result = r2ServiceClient.downloadObject(testBucketName, testObjectKey, null, "text");
        
        // 验证返回结果
        assertNotNull(result, "下载的对象内容不应为null");
        assertEquals(testObjectContent, result, "下载的内容应与上传的内容一致");
        
        System.out.println("下载的对象内容：" + result);
    }



    /**
     * 测试下载对象到文件
     */
    @Test
    @Order(5)
    public void testDownloadObjectToFile() {
        // 首先创建一个桶并上传一个对象
        r2ServiceClient.createBucket(testBucketName);
        r2ServiceClient.uploadObject(testBucketName, testObjectKey, testObjectContent, testContentType, "text");
        
        // 创建临时文件路径
        String tempFilePath = System.getProperty("java.io.tmpdir") + "/test-download-" + System.currentTimeMillis() + ".txt";
        
        // 调用downloadObject方法并指定保存到文件
        String result = r2ServiceClient.downloadObject(testBucketName, testObjectKey, tempFilePath, "file");
        
        // 验证返回结果
        assertNotNull(result, "下载结果不应为null");
        assertTrue(result.contains("downloaded to file"), "返回信息应表明文件已下载到本地");
        assertTrue(result.contains(tempFilePath), "返回信息应包含文件路径");
        
        System.out.println("下载对象到文件的结果：" + result);
    }

    /**
     * 测试获取对象元数据方法
     * 该方法用于获取存储桶中对象的元数据
     */
    @Test
    @Order(6)
    public void testGetObjectMetadata() {
        // 首先创建一个桶并上传一个对象
        r2ServiceClient.createBucket(testBucketName);
        r2ServiceClient.uploadObject(testBucketName, testObjectKey, testObjectContent, testContentType, "text");
        
        // 调用getObjectMetadata方法
        Map<String, String> metadata = r2ServiceClient.getObjectMetadata(testBucketName, testObjectKey);
        
        // 验证返回结果
        assertNotNull(metadata, "对象元数据不应为null");
        assertEquals(testContentType, metadata.get("contentType"), "内容类型应与上传时指定的一致");
        assertNotNull(metadata.get("contentLength"), "内容长度不应为null");
        
        System.out.println("对象元数据：" + metadata);
    }

    /**
     * 测试删除对象方法
     * 该方法用于从存储桶中删除对象
     */
    @Test
    @Order(7)
    public void testDeleteObject() {
        // 首先创建一个桶并上传一个对象
        r2ServiceClient.createBucket(testBucketName);
        r2ServiceClient.uploadObject(testBucketName, testObjectKey, testObjectContent, testContentType, "text");
        
        // 调用deleteObject方法
        String result = r2ServiceClient.deleteObject(testBucketName, testObjectKey);
        
        // 验证返回结果
        assertNotNull(result, "删除对象的结果不应为null");
        assertTrue(result.contains("deleted successfully"), "删除对象应返回成功信息");
        
        // 验证对象确实被删除了
        List<Map<String, String>> objects = r2ServiceClient.listObjects(testBucketName, "");
        assertTrue(objects.isEmpty(), "删除后桶中不应有对象");
        
        System.out.println("删除对象的结果：" + result);
    }

    /**
     * 测试删除桶方法
     * 该方法用于删除Cloudflare R2中的存储桶
     */
    @Test
    @Order(8)
    public void testDeleteBucket() {
        // 首先创建一个桶
        r2ServiceClient.createBucket(testBucketName);
        
        // 调用deleteBucket方法
        String result = r2ServiceClient.deleteBucket(testBucketName);
        
        // 验证返回结果
        assertNotNull(result, "删除桶的结果不应为null");
        assertTrue(result.contains("deleted successfully"), "删除桶应返回成功信息");
        
        // 验证桶确实被删除了
        List<String> buckets = r2ServiceClient.listBuckets();
        assertFalse(buckets.contains(testBucketName), "删除后桶列表中不应包含该桶");
        
        System.out.println("删除桶的结果：" + result);
    }
}