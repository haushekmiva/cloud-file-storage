package com.haushekmiva.cloudfilestorage.service;

import com.haushekmiva.cloudfilestorage.dto.ObjectInfo;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

        try {
            if (isDir(path)) {
                if (!isDirectoryExists(userPath)) throw new ResourceNotFoundException(userPath);
                downloadDirectory(userPath, outputStream);
            } else {
                if (!fileStorageService.isExists(userPath)) throw new ResourceNotFoundException(userPath);
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
        if (isDir(path)) {
            if (!isDirectoryExists(userPath)) throw new ResourceNotFoundException(userPath);
            fileStorageService.deleteObjects(userPath);
        } else {
            if (!fileStorageService.isExists(userPath)) throw new ResourceNotFoundException(userPath);
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


        if (isDir(path)) {

            if (!isDirectoryExists(userPath)) {
                throw new ResourceNotFoundException(userPath);
            }

            return new ResourceInfoResponse(pathParts.resourcePath(), pathParts.resourceName(), ResourceType.DIRECTORY);
        } else {

            if (!fileStorageService.isExists(userPath)) {
                throw new ResourceNotFoundException(userPath);
            }

            return new ResourceInfoResponse(pathParts.resourcePath(),
                    pathParts.resourceName(), ResourceType.FILE, fileStorageService.getObjectSize(userPath));
        }

    }

    @Override
    public List<ResourceInfoResponse> getDirectoryContentInfo(String path, Long userId) {

        if (!isPathValid(path)) {
            throw new InvalidPathException(path);
        }


        String userPath = getUserPath(path, userId);

        List<ObjectInfo> directoryContent = fileStorageService.getDirectoryTopLevelContent(userPath);

        if (directoryContent.isEmpty() && !fileStorageService.isExists(userPath)) {
            throw new ResourceNotFoundException(userPath);
        }

        List<ResourceInfoResponse> response = new ArrayList<>();

        for (ObjectInfo resource : directoryContent) {

            PathPartsDto pathParts = splitPath(removeUserPrefix(resource.path()));

            if (isDir(resource.path())) {
                response.add(new ResourceInfoResponse(pathParts.resourcePath(), pathParts.resourceName(), ResourceType.DIRECTORY));
            } else {
                response.add(new ResourceInfoResponse(pathParts.resourcePath(), pathParts.resourceName(), ResourceType.FILE,
                        resource.size()));
            }

        }
        return response;
    }

    @Override
    public ResourceInfoResponse createEmptyDirectory(String newDirectoryPath, Long userId) {

        String userPath = getUserPath(newDirectoryPath, userId);

        if (!isDir(newDirectoryPath)|| !isPathValid(newDirectoryPath)) {
            throw new InvalidPathException(newDirectoryPath);
        }

        PathPartsDto pathParts = splitPath(newDirectoryPath);

        String parentResourcePath = pathParts.resourcePath();
        if (!parentResourcePath.isEmpty() && !isDirectoryExists(getUserPath(parentResourcePath, userId))) {
            throw new ResourceNotFoundException(userPath);
        }

        if (isDirectoryExists(userPath)) {
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

        if (isDir(oldPath) != isDir(newPath)) {
            throw new InvalidPathException(oldPath);
        }

        PathPartsDto oldPathParts = splitPath(oldPath);
        PathPartsDto newPathParts = splitPath(newPath);

        String userOldPath = getUserPath(oldPath, userId);
        String userNewPath = getUserPath(newPath, userId);

        if (oldPath.equals(newPath)) {
            if (isDir(oldPath)) {
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

        if (isDir(oldPath)) {

            if (!isDirectoryExists(userOldPath)) {
                throw new ResourceNotFoundException(userOldPath);
            }

            if (isDirectoryExists(userNewPath)) {
                throw new ResourceAlreadyExistsException(userNewPath);
            }

            List<ObjectInfo> directoryContent = fileStorageService.getDirectoryContent(userOldPath);

            for (ObjectInfo resource : directoryContent) {
                String suffix = resource.path().substring(userOldPath.length());
                fileStorageService.copyObject(resource.path(), userNewPath + suffix);
            }
                fileStorageService.deleteObjects(userOldPath);

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

    @Override
    public List<ResourceInfoResponse> searchResource(String name, Long userId) {
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

                if (parts[i].contains(name) && addedDirectories.add(directoryPath)) {
                    PathPartsDto dirParts = splitPath(directoryPath);
                    searchResults.add(new ResourceInfoResponse(dirParts.resourcePath(), dirParts.resourceName(),
                            ResourceType.DIRECTORY));
                }
            }

            if (isDir(resource.path())) {
                PathPartsDto dirParts = splitPath(relativePath);
                if (dirParts.resourceName().contains(name) && addedDirectories.add(relativePath)) {
                    searchResults.add(new ResourceInfoResponse(dirParts.resourcePath(), dirParts.resourceName(),
                            ResourceType.DIRECTORY));
                }
                continue;
            }

            String fileName = parts[parts.length - 1];
            if (fileName.contains(name)) {
                PathPartsDto fileParts = splitPath(relativePath);
                searchResults.add(new ResourceInfoResponse(fileParts.resourcePath(), fileParts.resourceName(),
                        ResourceType.FILE, resource.size()));
            }
        }
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

    private String getUserPath(String path, Long userId) {
        return "user-" + userId + "-files/" + path;
    }

    private PathPartsDto splitPath(String fullPath) {
        String trimmed = isDir(fullPath) ? fullPath.substring(0, fullPath.length() - 1) : fullPath;
        int lastSlash = trimmed.lastIndexOf('/');
        if (lastSlash == -1) {
            return new PathPartsDto(fullPath, "");
        }

        String path = trimmed.substring(0, lastSlash + 1);
        String name = trimmed.substring(lastSlash + 1) + (isDir(fullPath) ? "/" : "");
        return new PathPartsDto(name, path);
    }

    private String removeUserPrefix(String userPath) {
        int firstSlash = userPath.indexOf('/');
        return userPath.substring(firstSlash + 1);
    }

    private boolean isPathValid(String path) {
        return path.length() < MAX_PATH_LENGTH && path.matches(VALID_PATH_REGEX);
    }

    private boolean isDir(String path) {
        return path.endsWith("/");
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
