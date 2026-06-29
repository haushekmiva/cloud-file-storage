package com.haushekmiva.cloudfilestorage.service;

import com.haushekmiva.cloudfilestorage.dto.ResourceInfoResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface ResourceService {

    List<ResourceInfoResponse> upload(List<MultipartFile> files, String path, Long userId);
    void download(String path, Long userId, OutputStream outputStream);
    void delete(String path, Long userId);

}
