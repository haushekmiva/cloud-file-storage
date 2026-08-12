package com.haushekmiva.cloudfilestorage.util;

import com.haushekmiva.cloudfilestorage.dto.PathPartsDto;

public final class PathUtils {

    private static final int MAX_PATH_LENGTH = 1024;
    private static final String VALID_PATH_REGEX = "^(?!/)(?!.*//)(?!.*(?:^|/)\\.+(?:/|$)).+$";

    private PathUtils() {}

    public static String getUserPath(String path, Long userId) {
        return "user-" + userId + "-files/" + path;
    }

    public static PathPartsDto splitPath(String fullPath) {
        String trimmed = isDir(fullPath) ? fullPath.substring(0, fullPath.length() - 1) : fullPath;
        int lastSlash = trimmed.lastIndexOf('/');
        if (lastSlash == -1) {
            return new PathPartsDto(trimmed, "");
        }

        String path = trimmed.substring(0, lastSlash + 1);
        String name = trimmed.substring(lastSlash + 1);
        return new PathPartsDto(name, path);
    }

    public static String removeUserPrefix(String userPath) {
        int firstSlash = userPath.indexOf('/');
        return userPath.substring(firstSlash + 1);
    }

    public static boolean isPathValid(String path) {
        return path.length() < MAX_PATH_LENGTH && path.matches(VALID_PATH_REGEX);
    }

    public static boolean isPathValidOrEmpty(String path) {
        return path != null && (path.isEmpty() || isPathValid(path));
    }

    public static boolean isDir(String path) {
        return path.endsWith("/");
    }
}
