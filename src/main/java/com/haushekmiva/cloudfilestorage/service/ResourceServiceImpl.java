package com.haushekmiva.cloudfilestorage.service;

import com.haushekmiva.cloudfilestorage.dto.ResourceInfoResponse;
import com.haushekmiva.cloudfilestorage.dto.ResourceType;
import com.haushekmiva.cloudfilestorage.exception.FileStorageException;
import com.haushekmiva.cloudfilestorage.exception.InvalidPathException;
import com.haushekmiva.cloudfilestorage.exception.ResourceAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceServiceImpl implements ResourceService {

    private static final int MAX_PATH_LENGTH = 1024;
    private static final String VALID_PATH_REGEX = "^(?!/)(?!.*//)(?!.*(?:^|/)\\.+(?:/|$))\\S+$";
    private final FileStorageService fileStorageService;

    // TODO: добавить макс размер файла
    @Override
    public List<ResourceInfoResponse> upload(List<MultipartFile> files, String path, Long userId) {

        if (!isPathValid(path) && !path.isEmpty()) {
            throw new InvalidPathException(path);
        }

        for (MultipartFile file : files) {
            String userPath = getUserPath(path + file.getOriginalFilename(), userId);

            if (!isPathValid(userPath)) {
                throw new InvalidPathException(userPath);
            }

            if (fileStorageService.isExists(userPath)) {
                throw new ResourceAlreadyExistsException(userPath);
            }
        }

        List<ResourceInfoResponse> response = new ArrayList<>();

        for (MultipartFile file : files) {
            String userPath = getUserPath(path + file.getOriginalFilename(), userId);

            try (InputStream inputStream = file.getInputStream()) {
                Long size = file.getSize();
                String contentType = file.getContentType();
                fileStorageService.upload(inputStream, userPath, size, contentType);
                log.info("Successful file upload: path={}, userId={}", userPath, userId);
                PathPartsDto parts = splitPath(path + file.getOriginalFilename());
                response.add(new ResourceInfoResponse(parts.filePath(), parts.fileName(), ResourceType.FILE, size));
            } catch (IOException e) {
                throw new FileStorageException("Error occurred while uploading file (%s).".formatted(userPath), e);
            }
        }
        return response;
    }

    @Override
    public void download(String path, Long userId, OutputStream outputStream) {
        if (!isPathValid(path)) {
            throw new InvalidPathException(path);
        }

        String userPath = getUserPath(path, userId);

        try {
            if (path.charAt(path.length() - 1) == '/') {
                downloadDirectory(userPath, outputStream);
            } else {
                downloadFile(userPath, outputStream);
            }
            log.info("Resource served: path={}", userPath);
        } catch (IOException e) {
            throw new FileStorageException("Error occurred while downloading file %s".formatted(userPath), e);
        }
    }

    @Override
    public void delete(String path, Long userId) {

    }

    private void downloadFile(String path, OutputStream outputStream) throws IOException {
        try (InputStream is = fileStorageService.download(path)) {
            is.transferTo(outputStream);
        }
    }

    private void downloadDirectory(String path, OutputStream outputStream) throws IOException {
        List<String> filesPath = fileStorageService.getDirectoryContent(path);

        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {

            for (String filePath : filesPath) {
                zos.putNextEntry(new ZipEntry(filePath.substring(path.length())));
                try (InputStream is = fileStorageService.download(filePath)) {
                    is.transferTo(zos);
                }
                zos.closeEntry();
            }
        }
    }


    private String getUserPath(String path, Long userId) {
        return "user-" + userId + "-files/" + path;
    }

    private PathPartsDto splitPath(String fullPath) {
        int lastSlash = fullPath.lastIndexOf('/');
        if (lastSlash == -1) {
            return new PathPartsDto("", fullPath);
        }
        String path = fullPath.substring(0, lastSlash + 1);
        String name = fullPath.substring(lastSlash + 1);
        return new PathPartsDto(name, path);
    }

    private boolean isPathValid(String path) {
        return path.length() < MAX_PATH_LENGTH && path.matches(VALID_PATH_REGEX);
    }

    private record PathPartsDto(
            String fileName,
            String filePath
    ) {
    }
}
