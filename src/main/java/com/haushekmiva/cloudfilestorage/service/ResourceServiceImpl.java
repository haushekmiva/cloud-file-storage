package com.haushekmiva.cloudfilestorage.service;

import com.haushekmiva.cloudfilestorage.dto.ResourceInfoResponse;
import com.haushekmiva.cloudfilestorage.dto.ResourceType;
import com.haushekmiva.cloudfilestorage.exception.FileStorageException;
import com.haushekmiva.cloudfilestorage.exception.InvalidPathException;
import com.haushekmiva.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.haushekmiva.cloudfilestorage.exception.ResourceNotFoundException;
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
                response.add(new ResourceInfoResponse(parts.resourcePath(), parts.resourceName(), ResourceType.FILE, size));
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

        if (!fileStorageService.isExists(userPath)) {
            throw new ResourceNotFoundException(userPath);
        }

        try {
            if (path.endsWith("/")) {
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

        if (!isPathValid(path)) {
            throw new InvalidPathException(path);
        }

        String userPath = getUserPath(path, userId);

        if (!fileStorageService.isExists(userPath)) {
            throw new ResourceNotFoundException(path);
        }

        if (path.endsWith("/")) {
            fileStorageService.deleteObjects(userPath);
        } else {
            fileStorageService.deleteObject(userPath);
        }

    }

    @Override
    public ResourceInfoResponse getResourceInfo(String path, Long userId) {
        if (!isPathValid(path)) {
            throw new InvalidPathException(path);
        }

        String userPath = getUserPath(path, userId);
        PathPartsDto pathParts = splitPath(path);


        if (path.endsWith("/")) {

            List<String> directoryContent = fileStorageService.getDirectoryContent(userPath);

            if (directoryContent.isEmpty()) {
                throw new ResourceNotFoundException(userPath);
            }

            return new ResourceInfoResponse(pathParts.resourcePath(), pathParts.resourceName(), ResourceType.DIRECTORY);
        } else {

            if (!fileStorageService.isExists(userPath)) {
                throw new ResourceNotFoundException(userPath);
            }

            return new ResourceInfoResponse(pathParts.resourcePath(),
                    pathParts.resourceName, ResourceType.FILE, fileStorageService.getObjectSize(userPath));
        }

    }

    @Override
    public List<ResourceInfoResponse> getDirectoryContentInfo(String path, Long userId) {

        if (!isPathValid(path)) {
            throw new InvalidPathException(path);
        }


        String userPath = getUserPath(path, userId);

        List<String> directoryContent = fileStorageService.getDirectoryTopLevelContent(userPath);

        if (!isDirectoryExists(userPath)) {
            throw new ResourceNotFoundException(userPath);
        }

        List<ResourceInfoResponse> response = new ArrayList<>();

        for (String resource : directoryContent) {

            PathPartsDto pathParts = splitPath(removeUserPrefix(resource));

            if (resource.endsWith("/")) {
                response.add(new ResourceInfoResponse(pathParts.resourcePath(), pathParts.resourceName(), ResourceType.DIRECTORY));
            } else {
                response.add(new ResourceInfoResponse(pathParts.resourcePath(), pathParts.resourceName(), ResourceType.FILE,
                        fileStorageService.getObjectSize(resource)));
            }

        }
        return response;
    }

    @Override
    public ResourceInfoResponse createEmptyDirectory(String newDirectoryPath, Long userId) {

        String userPath = getUserPath(newDirectoryPath, userId);

        if (!newDirectoryPath.endsWith("/") || !isPathValid(newDirectoryPath)) {
            throw new InvalidPathException(newDirectoryPath);
        }

        PathPartsDto pathParts = splitPath(newDirectoryPath);

        if (!isDirectoryExists(getUserPath(pathParts.resourcePath(), userId))) {
            throw new ResourceNotFoundException(userPath);
        }

        if (isDirectoryExists(newDirectoryPath)) {
            throw new ResourceAlreadyExistsException(userPath);
        }

        fileStorageService.createEmptyMarker(userPath);

        return new ResourceInfoResponse(pathParts.resourcePath(), pathParts.resourceName(), ResourceType.DIRECTORY);
    }

    @Override
    public ResourceInfoResponse moveResource(String oldPath, String newPath, Long userId) {

        if (!isPathValid(oldPath)) {
            throw new InvalidPathException(oldPath);
        }

        if (!isPathValid(newPath)) {
            throw new InvalidPathException(newPath);
        }

        if (oldPath.endsWith("/") != newPath.endsWith("/")) {
            throw new InvalidPathException(oldPath);
        }

        PathPartsDto oldPathParts = splitPath(oldPath);
        PathPartsDto newPathParts = splitPath(newPath);

        String userOldPath = getUserPath(oldPath, userId);
        String userNewPath = getUserPath(newPath, userId);

        if (oldPath.equals(newPath)) {
            if (oldPath.endsWith("/")) {
                return new ResourceInfoResponse(oldPathParts.resourcePath(), oldPathParts.resourceName(),
                        ResourceType.DIRECTORY);
            } else {
                return new ResourceInfoResponse(oldPathParts.resourcePath(), oldPathParts.resourceName(),
                        ResourceType.FILE, fileStorageService.getObjectSize(userOldPath));
            }
        }

        if (!oldPathParts.resourcePath().equals(newPathParts.resourcePath()) &&
                !oldPathParts.resourceName().equals(newPathParts.resourceName())) {
            throw new InvalidPathException(newPath);
        }

        if (oldPath.endsWith("/")) {

            if (!isDirectoryExists(userOldPath)) {
                throw new ResourceNotFoundException(userOldPath);
            }

            if (isDirectoryExists(userNewPath)) {
                throw new ResourceAlreadyExistsException(userNewPath);
            }

            List<String> directoryContent = fileStorageService.getDirectoryContent(userOldPath);

            for (String resource : directoryContent) {
                String suffix = resource.substring(userOldPath.length());
                fileStorageService.copyObject(resource, userNewPath + suffix);
                fileStorageService.deleteObject(resource);
            }

            return new ResourceInfoResponse(newPathParts.resourcePath(), newPathParts.resourceName(), ResourceType.DIRECTORY);

        } else {

            if (!fileStorageService.isExists(userOldPath)) {
                throw new ResourceNotFoundException(userOldPath);
            }

            if (fileStorageService.isExists(userNewPath)) {
                throw new ResourceAlreadyExistsException(userNewPath);
            }

            fileStorageService.copyObject(userOldPath, userNewPath);
            fileStorageService.deleteObject(userOldPath);
            return new ResourceInfoResponse(newPathParts.resourcePath(), newPathParts.resourceName(), ResourceType.FILE,
                    fileStorageService.getObjectSize(userNewPath));
        }
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
        boolean isDirectory = fullPath.endsWith("/");
        String trimmed = isDirectory ? fullPath.substring(0, fullPath.length() - 1) : fullPath;

        int lastSlash = trimmed.lastIndexOf('/');
        if (lastSlash == -1) {
            return new PathPartsDto(fullPath, "");
        }

        String path = trimmed.substring(0, lastSlash + 1);
        String name = trimmed.substring(lastSlash + 1) + (isDirectory ? "/" : "");
        return new PathPartsDto(name, path);
    }

    private String removeUserPrefix(String userPath) {
        int firstSlash = userPath.indexOf('/');
        return userPath.substring(firstSlash + 1);
    }

    private boolean isPathValid(String path) {
        return path.length() < MAX_PATH_LENGTH && path.matches(VALID_PATH_REGEX);
    }

    private boolean isDirectoryExists(String path) {
        return fileStorageService.isExists(path) || !fileStorageService.getDirectoryTopLevelContent(path).isEmpty();
    }

    private record PathPartsDto(
            String resourceName,
            String resourcePath
    ) {
    }
}
