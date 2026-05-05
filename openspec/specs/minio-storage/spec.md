## ADDED Requirements

### Requirement: MinIO 客户端配置

系统 SHALL 提供 MinioConfig 配置类，从 application.yml 读取 MinIO endpoint、accessKey、secretKey，创建 MinioClient Bean。开发环境 endpoint 为 http://localhost:9000。

#### Scenario: MinioClient 创建

- **WHEN** Spring Boot 应用启动
- **THEN** MinioClient Bean 成功创建，可连接到 localhost:9000

### Requirement: MinIO Bucket 自动创建

MinioConfig SHALL 在 @PostConstruct 中检查配置的 bucket 是否存在，不存在则自动创建。该操作 SHALL 为幂等，重复执行无副作用。

#### Scenario: 首次启动创建 bucket

- **WHEN** 系统首次启动，配置的 bucket 不存在
- **THEN** MinioConfig 自动创建该 bucket

#### Scenario: 重复启动不重复创建

- **WHEN** 系统第二次启动，bucket 已存在
- **THEN** MinioConfig 检测到 bucket 存在，跳过创建操作
