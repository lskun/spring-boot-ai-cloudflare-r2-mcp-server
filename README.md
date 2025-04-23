# Spring Boot AI Cloudflare R2 MCP Server

English | [简体中文](README.zh-CN.md)

## Introduction
Spring Boot AI Cloudflare R2 MCP Server is a Model Context Protocol (MCP) server implementation based on Spring Boot and Spring AI, providing integration with Cloudflare R2 object storage service.

## Features
- Complete Cloudflare R2 object storage operation support
- Integration with Spring AI's MCP server
- Support for various file types (text, binary, etc.)
- Comprehensive test coverage
- Easy configuration and deployment

## Prerequisites
- JDK 17 or above
- Maven 3.6 or above
- Cloudflare R2 account and credentials

## Configuration
Create or modify `application.properties` with your R2 credentials:
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

4. Run integration tests:
```bash
# Run all tests in R2ServiceClientTest
mvn test -Dtest=R2ServiceClientTest

# Or run a specific test method
mvn test -Dtest=R2ServiceClientTest#testListBuckets
```

The test suite includes comprehensive tests for all R2 operations:
- Bucket operations (list, create, delete)
- Object operations (upload, download, list, delete)
- Content format handling (text, base64, file path)
- Object metadata management

## API Reference
The service provides the following main functionalities:
- List buckets
- Create/Delete buckets
- Upload/Download objects
- List objects in bucket
- Get object metadata
- Delete objects

## Development
The project uses Spring Boot 3.4.4 and Spring AI 1.0.0-M6. Main components include:
- `R2ServiceClient`: Core service class for R2 operations
- `McpConfiguration`: MCP tool configuration
- Integration tests for all operations

## Contributing
1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request

## License
This project is licensed under the MIT License - see the LICENSE file for details.

## Contact
If you have any questions or suggestions, please feel free to create an issue. 