package com.haushekmiva.cloudfilestorage.service;

import com.haushekmiva.cloudfilestorage.exception.FileStorageException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Override
    public void upload(InputStream data, String key, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(data, size, (long) -1)
                    .contentType(contentType)
                    .build()
            );
        } catch (MinioException e) {
            throw new FileStorageException("Error occurred while uploading file.", e.getCause());
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );

        } catch (MinioException e) {
            throw new FileStorageException("Error occurred while downloading file", e.getCause());
        }
    }

    @Override
    public void delete(String key) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
        } catch (MinioException e) {
            throw new FileStorageException("Error occurred while removing file", e.getCause());
        }
    }
}
