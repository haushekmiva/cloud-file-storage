package com.haushekmiva.cloudfilestorage.controller;

import com.haushekmiva.cloudfilestorage.dto.ResourceInfoResponse;
import com.haushekmiva.cloudfilestorage.dto.ResourceType;
import com.haushekmiva.cloudfilestorage.dto.SearchRequest;
import com.haushekmiva.cloudfilestorage.security.UserDetailsImpl;
import com.haushekmiva.cloudfilestorage.service.ResourceService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class FileStorageController {

    private final ResourceService resourceService;

    @GetMapping("/resource")
    public ResponseEntity<ResourceInfoResponse> getResourceInfo(@RequestParam String path,
                                                                @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ResourceInfoResponse response = resourceService.getResourceInfo(path, userDetails.user().getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resource")
    public ResponseEntity<List<ResourceInfoResponse>> uploadResource(@RequestParam String path,
                                                                     @RequestParam("files") List<MultipartFile> files,
                                                                     @AuthenticationPrincipal UserDetailsImpl userDetails) {

        List<ResourceInfoResponse> responses = resourceService.upload(files, path, userDetails.user().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    // и закрыть все оставшиеся эндпоинты
    @GetMapping("/resource/download")
    public void downloadResource(@RequestParam String path,
                                 HttpServletResponse response,
                                 @AuthenticationPrincipal UserDetailsImpl userDetails) throws IOException {

        ResourceInfoResponse resourceInfo = resourceService.getResourceInfo(path, userDetails.user().getId());

        String fileName = resourceInfo.name();
        if (resourceInfo.type() == ResourceType.DIRECTORY) {
            fileName += ".zip";
        }

        ContentDisposition contentDisposition = ContentDisposition
                .attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();

        response.setContentType(MediaType.APPLICATION_OCTET_STREAM.toString());
        response.setContentLengthLong(resourceInfo.size());
        response.setStatus(HttpStatus.OK.value());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        resourceService.download(path, userDetails.user().getId(), response.getOutputStream());
    }

    @DeleteMapping("/resource")
    public ResponseEntity<Void> deleteResource(@RequestParam String path,
                                               @AuthenticationPrincipal UserDetailsImpl userDetails) {
        resourceService.delete(path, userDetails.user().getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resource")
    public ResponseEntity<ResourceInfoResponse> moveResource(@RequestParam String from,
                                                             @RequestParam String to,
                                                             @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ResourceInfoResponse response = resourceService.moveResource(from, to, userDetails.user().getId());
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/directory")
    public ResponseEntity<List<ResourceInfoResponse>> getDirectoryContent(@RequestParam String path,
                                                                    @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<ResourceInfoResponse> responses = resourceService.getDirectoryContentInfo(path, userDetails.user().getId());
        return ResponseEntity.ok().body(responses);
    }

    @PostMapping("/directory")
    public ResponseEntity<ResourceInfoResponse> createEmptyDirectory(@RequestParam String path,
                                                                     @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ResourceInfoResponse response = resourceService.createEmptyDirectory(path, userDetails.user().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/resource/search")
    public ResponseEntity<List<ResourceInfoResponse>> searchResources(@Valid SearchRequest searchRequest,
                                                                      @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<ResourceInfoResponse> searchResults = resourceService.searchResource(
                searchRequest.query(), userDetails.user().getId());
        return ResponseEntity.ok().body(searchResults);
    }
}
