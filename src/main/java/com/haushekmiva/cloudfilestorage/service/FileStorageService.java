package com.haushekmiva.cloudfilestorage.service;

import io.minio.Result;
import io.minio.messages.Item;

import java.io.InputStream;
import java.util.List;

public interface FileStorageService {

    void upload(InputStream data, String key, long size, String contentType);
    InputStream download(String key);
    void delete(String key);
    boolean isExists(String key);

    List<String> getDirectoryContent(String prefix);
}
