package com.haushekmiva.cloudfilestorage.service;

import com.haushekmiva.cloudfilestorage.dto.ObjectInfo;
import com.haushekmiva.cloudfilestorage.dto.PathPartsDto;
import com.haushekmiva.cloudfilestorage.dto.ResourceInfoResponse;
import com.haushekmiva.cloudfilestorage.dto.ResourceType;
import com.haushekmiva.cloudfilestorage.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.haushekmiva.cloudfilestorage.util.PathUtils.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceServiceImpl implements ResourceService {

    private final FileStorageService fileStorageService;

    @Override
    public List<ResourceInfoResponse> upload(List<MultipartFile> files, String path, Long userId) {

        ensureValidPathOrEmpty(path);

        if (files == null || files.isEmpty()) {
            throw new FilesNotUploadedException("The list of files provided for upload is empty.");
        }

        for (MultipartFile file : files) {
            String filePath = path + file.getOriginalFilename();
            String userPath = getUserPath(filePath, userId);

            ensureValidPath(userPath);
            ensureFileNotExists(filePath, userId);
        }

        List<ResourceInfoResponse> response = new ArrayList<>();

        for (MultipartFile file : files) {
            String filePath = path + file.getOriginalFilename();
            String userPath = getUserPath(filePath, userId);

            try (InputStream inputStream = file.getInputStream()) {
                long size = file.getSize();
                String contentType = file.getContentType();
                fileStorageService.upload(inputStream, userPath, size, contentType);
                log.info("Successful file upload: path={}, userId={}", userPath, userId);
                PathPartsDto parts = splitPath(filePath);
                response.add(new ResourceInfoResponse(parts.resourcePath(), parts.resourceName(), ResourceType.FILE, size));
            } catch (IOException e) {
                throw new FileStorageException("Error occurred while uploading file (%s).".formatted(userPath), e);
            }
        }
        return response;
    }

    @Override
    public void download(String path, Long userId, OutputStream outputStream) {

        ensureValidPathOrEmpty(path);
        String userPath = getUserPath(path, userId);

        try {
            if (isDir(path) || path.isEmpty()) {
                ensureDirectoryExists(path, userId);
                downloadDirectory(userPath, outputStream);
            } else {
                ensureFileExists(path, userId);
                downloadFile(userPath, outputStream);
            }
            log.info("Resource served: path={} userId={}", userPath, userId);
        } catch (IOException e) {
            throw new FileStorageException("Error occurred while downloading file %s".formatted(userPath), e);
        }
    }

    @Override
    public void delete(String path, Long userId) {

        // нельзя удалить корневую директорию
        ensureValidPath(path);

        String userPath = getUserPath(path, userId);
        if (isDir(path)) {
            ensureDirectoryExists(path, userId);
            fileStorageService.deleteObjects(userPath);
            log.info("Directory deleted: path={} userId={}", userPath, userId);
        } else {
            ensureFileExists(path, userId);
            fileStorageService.deleteObject(userPath);
            log.info("File deleted: path={} userId={}", userPath, userId);
        }

        restoreDirectoryMarkerIfEmpty(path, userId);
    }

    @Override
    public ResourceInfoResponse getResourceInfo(String path, Long userId) {
        ensureValidPath(path);

        String userPath = getUserPath(path, userId);
        PathPartsDto pathParts = splitPath(path);

        if (isDir(path)) {
            ensureDirectoryExists(path, userId);

            log.info("Directory information requested: path={} userId={}", userPath, userId);
            return new ResourceInfoResponse(pathParts.resourcePath(), pathParts.resourceName(), ResourceType.DIRECTORY);
        } else {
            ensureFileExists(path, userId);

            log.info("File information requested: path={} userId={}", userPath, userId);
            return new ResourceInfoResponse(pathParts.resourcePath(),
                    pathParts.resourceName(), ResourceType.FILE, fileStorageService.getObjectSize(userPath));
        }
    }

    @Override
    public List<ResourceInfoResponse> getDirectoryContentInfo(String path, Long userId) {

        ensureValidPathOrEmpty(path);

        String userPath = getUserPath(path, userId);

        List<ObjectInfo> directoryContent = fileStorageService.getDirectoryTopLevelContent(userPath);

        if (directoryContent.isEmpty() && !path.isEmpty() && !fileStorageService.isExists(userPath)) {
            throw new ResourceNotFoundException(userPath);
        }

        List<ResourceInfoResponse> response = new ArrayList<>();

        for (ObjectInfo resource : directoryContent) {

            PathPartsDto pathParts = splitPath(removeUserPrefix(resource.path()));

            if (isDir(resource.path())) {
                if (!removeUserPrefix(resource.path()).equals(path)) {
                    response.add(new ResourceInfoResponse(pathParts.resourcePath(), pathParts.resourceName(), ResourceType.DIRECTORY));
                }
            } else {
                response.add(new ResourceInfoResponse(pathParts.resourcePath(), pathParts.resourceName(), ResourceType.FILE,
                        resource.size()));
            }
        }

        log.info("Directory content requested: path={} userId={}", userPath, userId);
        return response;
    }

    @Override
    public ResourceInfoResponse createEmptyDirectory(String path, Long userId) {

        ensureValidPath(path);

        if (!isDir(path)) {
            throw new InvalidPathException(path);
        }

        String userPath = getUserPath(path, userId);
        PathPartsDto pathParts = splitPath(path);

        String parentResourcePath = pathParts.resourcePath();
        if (!parentResourcePath.isEmpty()) {
            ensureDirectoryExists(parentResourcePath, userId);
        }

        ensureDirectoryNotExists(path, userId);

        fileStorageService.createEmptyMarker(userPath);

        log.info("Empty directory created: path={} userId={}", userPath, userId);
        return new ResourceInfoResponse(pathParts.resourcePath(), pathParts.resourceName(), ResourceType.DIRECTORY);
    }

    @Override
    public ResourceInfoResponse moveResource(String oldPath, String newPath, Long userId) {

        if (oldPath.equals(newPath)) {
            return getResourceInfo(oldPath, userId);
        }

        ensureValidPath(oldPath);
        ensureValidPath(newPath);

        if (isDir(oldPath) != isDir(newPath)) {
            throw new InvalidPathException(oldPath);
        }

        if (isDir(oldPath) && newPath.startsWith(oldPath)) {
            throw new InvalidPathException(newPath);
        }

        PathPartsDto oldPathParts = splitPath(oldPath);
        PathPartsDto newPathParts = splitPath(newPath);

        String userOldPath = getUserPath(oldPath, userId);
        String userNewPath = getUserPath(newPath, userId);

        if (!oldPathParts.resourcePath().equals(newPathParts.resourcePath()) &&
                !oldPathParts.resourceName().equals(newPathParts.resourceName())) {
            throw new InvalidPathException(newPath);
        }



        if (isDir(oldPath)) {
            List<String> copiedResources = new ArrayList<>();
            ensureDirectoryExists(oldPath, userId);
            ensureDirectoryNotExists(newPath, userId);

            List<ObjectInfo> directoryContent = fileStorageService.getDirectoryContent(userOldPath);

            try {
                for (ObjectInfo resource : directoryContent) {
                    String suffix = resource.path().substring(userOldPath.length());
                    fileStorageService.copyObject(resource.path(), userNewPath + suffix);
                    copiedResources.add(userNewPath + suffix);
                }
            } catch (Exception copyException) {
                for (String resource : copiedResources) {
                    try {
                        fileStorageService.deleteObject(resource);
                    } catch (Exception deleteException) {
                        log.error("Can not delete copied file in rollback: path = {}", resource, deleteException);
                    }
                }
                throw copyException;
            }
            fileStorageService.deleteObjects(userOldPath);

            log.info("Directory moved: from={} to={} userId={}", userOldPath, userNewPath, userId);
            restoreDirectoryMarkerIfEmpty(oldPath, userId);
            return new ResourceInfoResponse(newPathParts.resourcePath(), newPathParts.resourceName(), ResourceType.DIRECTORY);

        } else {
            ensureFileExists(oldPath, userId);
            ensureFileNotExists(newPath, userId);

            fileStorageService.copyObject(userOldPath, userNewPath);

            try {
                fileStorageService.deleteObject(userOldPath);
            } catch (Exception deleteOldException) {
                try {
                    fileStorageService.deleteObject(userNewPath);
                } catch (Exception rollbackException) {
                    log.error("Can not delete copied file in rollback: path = {}", userNewPath, rollbackException);
                }
                throw deleteOldException;
            }
            log.info("File moved: from={} to={} userId={}", userOldPath, userNewPath, userId);
            restoreDirectoryMarkerIfEmpty(oldPath, userId);
            return new ResourceInfoResponse(newPathParts.resourcePath(), newPathParts.resourceName(), ResourceType.FILE,
                    fileStorageService.getObjectSize(userNewPath));
        }
    }

    @Override
    public List<ResourceInfoResponse> searchResource(String query, Long userId) {
        String userPath = getUserPath("", userId);
        List<ObjectInfo> userResources = fileStorageService.searchObjects(userPath);

        List<ResourceInfoResponse> searchResults = new ArrayList<>();
        Set<String> addedDirectories = new HashSet<>();

        for (ObjectInfo resource : userResources) {
            String relativePath = removeUserPrefix(resource.path());
            String[] parts = relativePath.split("/");

            StringBuilder current = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                current.append(parts[i]).append("/");
                String directoryPath = current.toString();

                if (parts[i].contains(query) && addedDirectories.add(directoryPath)) {
                    PathPartsDto dirParts = splitPath(directoryPath);
                    searchResults.add(new ResourceInfoResponse(dirParts.resourcePath(), dirParts.resourceName(),
                            ResourceType.DIRECTORY));
                }
            }

            if (isDir(resource.path())) {
                PathPartsDto dirParts = splitPath(relativePath);
                if (dirParts.resourceName().contains(query) && addedDirectories.add(relativePath)) {
                    searchResults.add(new ResourceInfoResponse(dirParts.resourcePath(), dirParts.resourceName(),
                            ResourceType.DIRECTORY));
                }
                continue;
            }

            String fileName = parts[parts.length - 1];
            if (fileName.contains(query)) {
                PathPartsDto fileParts = splitPath(relativePath);
                searchResults.add(new ResourceInfoResponse(fileParts.resourcePath(), fileParts.resourceName(),
                        ResourceType.FILE, resource.size()));
            }
        }

        log.info("Resource search request: query={} userId={}", query, userId);
        return searchResults;
    }

    private void downloadFile(String path, OutputStream outputStream) throws IOException {
        try (InputStream is = fileStorageService.download(path)) {
            is.transferTo(outputStream);
        }
    }

    private void downloadDirectory(String path, OutputStream outputStream) throws IOException {
        List<ObjectInfo> filesPath = fileStorageService.getDirectoryContent(path);

        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            for (ObjectInfo resource : filesPath) {
                String zipEntryPath = resource.path().substring(path.length());

                if (zipEntryPath.isEmpty()) {
                    continue;
                }
                if (isDir(resource.path())) {
                    zos.putNextEntry(new ZipEntry(zipEntryPath));
                    zos.closeEntry();
                } else {
                    zos.putNextEntry(new ZipEntry(zipEntryPath));
                    try (InputStream is = fileStorageService.download(resource.path())) {
                        is.transferTo(zos);
                    }
                    zos.closeEntry();
                }
            }
        }
    }

    private void ensureValidPath(String path) {
        if (!isPathValid(path)) {
            throw new InvalidPathException(path);
        }
    }

    private void ensureValidPathOrEmpty(String path) {
        if (!isPathValidOrEmpty(path)) {
            throw new InvalidPathException(path);
        }
    }

    private void ensureDirectoryExists(String path, Long userId) {
        if (!isDirectoryExists(path, userId)) {
            throw new ResourceNotFoundException(getUserPath(path, userId));
        }
    }

    private void ensureDirectoryNotExists(String path, Long userId) {
        if (isDirectoryExists(path, userId)) {
            throw new ResourceAlreadyExistsException(getUserPath(path, userId));
        }
    }

    private void ensureFileExists(String path, Long userId) {
        String userPath = getUserPath(path, userId);
        if (!fileStorageService.isExists(userPath)) {
            throw new ResourceNotFoundException(userPath);
        }
    }

    private void ensureFileNotExists(String path, Long userId) {
        String userPath = getUserPath(path, userId);
        if (fileStorageService.isExists(userPath)) {
            throw new ResourceAlreadyExistsException(userPath);
        }
    }

    private void restoreDirectoryMarkerIfEmpty(String path, Long userId) {
        PathPartsDto pathParts = splitPath(path);
        if (!pathParts.resourcePath().isEmpty()) {
            if (!isDirectoryExists(pathParts.resourcePath(), userId)) {
                fileStorageService.createEmptyMarker(getUserPath(pathParts.resourcePath(), userId));
            }
        }
    }

    private boolean isDirectoryExists(String path, Long userId) {
        String userPath = getUserPath(path, userId);
        return fileStorageService.isExists(userPath) || !fileStorageService.getDirectoryTopLevelContent(userPath).isEmpty();
    }
}