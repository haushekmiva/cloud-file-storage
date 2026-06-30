package com.haushekmiva.cloudfilestorage.service;

import java.io.InputStream;
import java.util.List;

public interface FileStorageService {

    void upload(InputStream data, String key, long size, String contentType);

    InputStream download(String key);

    void deleteObject(String key);

    void deleteObjects(String prefix);

    boolean isExists(String key);

    List<String> getDirectoryContent(String prefix);
}
