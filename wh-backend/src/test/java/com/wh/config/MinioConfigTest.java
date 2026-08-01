package com.wh.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("MinioConfig 测试")
class MinioConfigTest {

    private MinioConfig newConfig() {
        MinioConfig config = new MinioConfig();
        ReflectionTestUtils.setField(config, "endpoint", "http://localhost:9010");
        ReflectionTestUtils.setField(config, "accessKey", "minioadmin");
        ReflectionTestUtils.setField(config, "secretKey", "minioadmin123");
        ReflectionTestUtils.setField(config, "bucket", "wh-files");
        return config;
    }

    @Test
    @DisplayName("bucket 不存在时创建 bucket")
    void bucketNotExists_makesBucket() throws Exception {
        MinioConfig config = newConfig();
        MinioClient client = mock(MinioClient.class);
        when(client.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        ReflectionTestUtils.invokeMethod(config, "initBucket", client);

        verify(client).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("bucket 已存在时不重复创建")
    void bucketExists_doesNotMakeBucket() throws Exception {
        MinioConfig config = newConfig();
        MinioClient client = mock(MinioClient.class);
        when(client.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        ReflectionTestUtils.invokeMethod(config, "initBucket", client);

        verify(client, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("MinIO 不可达时静默降级不抛出")
    void bucketCheckFails_swallowsException() throws Exception {
        MinioConfig config = newConfig();
        MinioClient client = mock(MinioClient.class);
        when(client.bucketExists(any(BucketExistsArgs.class)))
                .thenThrow(new RuntimeException("connection refused"));

        ReflectionTestUtils.invokeMethod(config, "initBucket", client);
    }

    @Test
    @DisplayName("minioClient bean 可创建（MinIO 不可达时降级）")
    void minioClient_beanCreatable() {
        MinioConfig config = newConfig();
        MinioClient client = config.minioClient();
        assertNotNull(client);
    }
}
