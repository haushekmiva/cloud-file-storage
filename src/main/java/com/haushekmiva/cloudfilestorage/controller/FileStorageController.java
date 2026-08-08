package com.haushekmiva.cloudfilestorage.controller;

import com.haushekmiva.cloudfilestorage.dto.ResourceInfoResponse;
import com.haushekmiva.cloudfilestorage.security.UserDetailsImpl;
import com.haushekmiva.cloudfilestorage.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/resource")
@RequiredArgsConstructor
@Slf4j
public class FileStorageController {

    private final ResourceService resourceService;

    @GetMapping
    public ResponseEntity<ResourceInfoResponse> getResourceInfo(@RequestParam String path,
                                                                @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ResourceInfoResponse response = resourceService.getResourceInfo(path, userDetails.user().getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<List<ResourceInfoResponse>> uploadResource(@RequestParam String path,
                                                                     @RequestParam("files") List<MultipartFile> files,
                                                                     @AuthenticationPrincipal UserDetailsImpl userDetails) {

        List<ResourceInfoResponse> responses = resourceService.upload(files, path, userDetails.user().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }
}
