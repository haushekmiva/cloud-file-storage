package com.haushekmiva.cloudfilestorage.service;

import java.io.InputStream;

public interface FileStorageService {

    void upload(InputStream data, String key, long size, String contentType);
    InputStream download(String key);
    void delete(String key);
    boolean isExists(String key);

}
