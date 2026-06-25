package com.haushekmiva.cloudfilestorage.service;

import java.io.InputStream;

public interface FileStorageService {

    public void upload(InputStream data, String key, long size, String contentType);
    public InputStream download(String key);
    public void delete(String key);

}
