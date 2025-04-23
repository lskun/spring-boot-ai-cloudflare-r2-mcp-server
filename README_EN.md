# Spring Boot AI Cloudflare R2 MCP Server

[简体中文](README.md) | English

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

4. Run tests:
```bash
./test-r2-service-client.sh
```

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