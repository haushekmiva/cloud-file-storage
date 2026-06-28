package com.haushekmiva.cloudfilestorage.service;

import com.haushekmiva.cloudfilestorage.dto.ResourceInfoResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface ResourceService {

    List<ResourceInfoResponse> upload(List<MultipartFile> files, String path, Long userId);
    InputStream download(String path, Long userId);
    void delete(String path, Long userId);

}
