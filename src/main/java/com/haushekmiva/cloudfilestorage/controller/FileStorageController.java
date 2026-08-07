package com.haushekmiva.cloudfilestorage.controller;

import com.haushekmiva.cloudfilestorage.dto.ResourceInfoResponse;
import com.haushekmiva.cloudfilestorage.security.UserDetailsImpl;
import com.haushekmiva.cloudfilestorage.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

}
