package com.haushekmiva.cloudfilestorage.service;

import com.haushekmiva.cloudfilestorage.dto.ObjectInfo;

import java.io.InputStream;
import java.util.List;

public interface FileStorageService {

    void upload(InputStream data, String key, long size, String contentType);

    InputStream download(String key);

    void deleteObject(String key);

    void deleteObjects(String prefix);

    boolean isExists(String key);

    Long getObjectSize(String key);

    List<ObjectInfo> getDirectoryContent(String prefix);

    List<ObjectInfo> getDirectoryTopLevelContent(String prefix);

    void createEmptyMarker(String key);

    void copyObject(String key, String copyKey);

    List<ObjectInfo> searchObjects(String prefix);
}
