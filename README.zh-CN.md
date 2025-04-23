# Spring Boot AI Cloudflare R2 MCP Server

[English](README.md) | 简体中文

这是一个基于Spring Boot AI的Cloudflare R2 MCP服务器，提供了与Cloudflare R2对象存储交互的工具。

## 项目结构

```
src/main/java/com/bootcamptoprod/
├── SpringBootAiCloudflareR2McpServerApplication.java (应用程序入口)
├── config/
│   └── McpConfiguration.java (MCP配置类)
└── service/
    └── R2ServiceClient.java (R2服务客户端)
```

## 配置

在`application.properties`中配置以下属性：

```properties
# Cloudflare R2 配置
r2.access-key-id=${R2_ACCESS_KEY_ID}
r2.secret-access-key=${R2_SECRET_ACCESS_KEY}
r2.endpoint=${R2_ENDPOINT}
```

请确保在运行应用程序之前设置以下环境变量：
- `R2_ACCESS_KEY_ID`: Cloudflare R2的访问密钥ID
- `R2_SECRET_ACCESS_KEY`: Cloudflare R2的秘密访问密钥
- `R2_ENDPOINT`: Cloudflare R2的端点URL (例如: https://accountid.r2.cloudflarestorage.com)

## 可用工具

该服务器提供以下Cloudflare R2操作工具：

- `listBuckets`: 列出Cloudflare R2中的所有存储桶
- `createBucket`: 在Cloudflare R2中创建新的存储桶
- `deleteBucket`: 从Cloudflare R2中删除存储桶
- `listObjects`: 列出存储桶中的对象
- `uploadObject`: 将对象上传到存储桶
- `downloadObject`: 从存储桶下载对象
- `deleteObject`: 从存储桶删除对象
- `getObjectMetadata`: 获取对象元数据

## 构建与运行

### 构建项目

```bash
mvn clean package
```

### 运行测试

```bash
# 运行R2ServiceClientTest中的所有测试
mvn test -Dtest=R2ServiceClientTest

# 或运行特定的测试方法
mvn test -Dtest=R2ServiceClientTest#testListBuckets
```

测试套件包含了所有R2操作的完整测试：
- 存储桶操作（列表、创建、删除）
- 对象操作（上传、下载、列表、删除）
- 内容格式处理（文本、base64、文件路径）
- 对象元数据管理

### 运行应用程序

```bash
java -jar target/spring-boot-ai-cloudflare-r2-mcp-server-0.0.1-SNAPSHOT.jar
```

## 使用示例

在Spring AI应用程序中，可以通过以下方式使用此MCP服务器：

```java
@Autowired
private McpClient mcpClient;

public void useR2Tools() {
    // 列出所有存储桶
    List<String> buckets = mcpClient.call("listBuckets", new HashMap<>(), new TypeReference<List<String>>() {});
    System.out.println("Buckets: " + buckets);
    
    // 上传对象
    Map<String, Object> params = new HashMap<>();
    params.put("bucketName", "my-bucket");
    params.put("key", "test.txt");
    params.put("content", "Hello, Cloudflare R2!");
    params.put("contentType", "text/plain");
    params.put("contentFormat", "text");
    String result = mcpClient.call("uploadObject", params, String.class);
    System.out.println(result);
}
```