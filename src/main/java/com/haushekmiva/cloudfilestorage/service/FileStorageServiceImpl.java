package com.haushekmiva.cloudfilestorage.service;

import com.haushekmiva.cloudfilestorage.exception.FileStorageException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
                    .stream(data, size, -1L)
                    .contentType(contentType)
                    .build()
            );
        } catch (MinioException e) {
            throw new FileStorageException("Error occurred while uploading file.", e);
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
            throw new FileStorageException("Error occurred while downloading file", e);
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
            throw new FileStorageException("Error occurred while removing file", e);
        }
    }

    @Override
    public boolean isExists(String key) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            } else {
                throw new FileStorageException("Unknown file storage error occurred.", e);
            }
        } catch (MinioException e) {
            throw new FileStorageException("Unknown file storage error occurred.", e);
        }
    }

    @Override
    public List<String> getDirectoryContent(String prefix) {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .prefix(prefix)
                        .recursive(true)
                        .build()
        );

        List<String> objectNames = new ArrayList<>();

        try {
            for (Result<Item> result : results) {
                Item item = result.get();
                objectNames.add(item.objectName());
            }
        } catch (MinioException e) {
            throw new FileStorageException("Error occurred while getting object names", e);
        }
        return objectNames;
    }

}
