# Spring Boot AI Cloudflare R2 MCP Server

[简体中文](README.md) | English

## Introduction
Spring Boot AI Cloudflare R2 MCP Server is a Model Context Protocol (MCP) server implementation based on Spring Boot and Spring AI, providing integration with Cloudflare R2 object storage service.

## Features
- Complete Cloudflare R2 object storage service integration
- Support for all file types (text and binary)
- Streaming upload and download capabilities
- Comprehensive error handling and logging
- Integration with Spring AI MCP Server

## Prerequisites
- JDK 17 or above
- Maven 3.6 or above
- Cloudflare R2 account and credentials

## Configuration
Configure your Cloudflare R2 credentials in `application.properties`:

```properties
r2.access-key-id=${R2_ACCESS_KEY_ID}
r2.secret-access-key=${R2_SECRET_ACCESS_KEY}
r2.endpoint=${R2_ENDPOINT}
```

## Quick Start

1. Clone the repository:
```bash
git clone https://github.com/lskun/spring-boot-ai-cloudflare-r2-mcp-server.git
```

2. Set up environment variables:
```bash
export R2_ACCESS_KEY_ID=your_access_key_id
export R2_SECRET_ACCESS_KEY=your_secret_access_key
export R2_ENDPOINT=your_r2_endpoint
```

3. Build the project:
```bash
mvn clean package
```

4. Run the tests:
```bash
./test-r2-service-client.sh
```

## API Reference

### List Buckets
```java
List<String> buckets = r2ServiceClient.listBuckets();
```

### Create Bucket
```java
String result = r2ServiceClient.createBucket("my-bucket");
```

### Upload Object
```java
String result = r2ServiceClient.uploadObject(
    "my-bucket",
    "my-file.txt",
    "Hello, World!",
    "text/plain",
    "text"
);
```

### Download Object
```java
String content = r2ServiceClient.downloadObject(
    "my-bucket",
    "my-file.txt",
    null,
    "text"
);
```

### Delete Object
```java
String result = r2ServiceClient.deleteObject("my-bucket", "my-file.txt");
```

## Contributing
Contributions are welcome! Please feel free to submit a Pull Request.

## License
This project is licensed under the MIT License - see the LICENSE file for details.