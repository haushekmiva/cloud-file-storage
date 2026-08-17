package com.haushekmiva.cloudfilestorage.controller;

import com.haushekmiva.cloudfilestorage.dto.ErrorResponse;
import com.haushekmiva.cloudfilestorage.dto.ResourceInfoResponse;
import com.haushekmiva.cloudfilestorage.dto.ResourceType;
import com.haushekmiva.cloudfilestorage.dto.SearchRequest;
import com.haushekmiva.cloudfilestorage.security.UserDetailsImpl;
import com.haushekmiva.cloudfilestorage.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@SecurityRequirement(name = "sessionAuth")
@Tag(name = "Resource", description = "File storage resource management")
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class FileStorageController {

    private final ResourceService resourceService;

    @GetMapping("/resource")
    @Operation(
            summary = "Get resource information",
            description = "Gets resource path, name, type, and size (if it is a file)"
    )
    @ApiResponse(responseCode = "200", description = "Resource information retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid path",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Resource not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ResourceInfoResponse> getResourceInfo(@RequestParam String path,
                                                                @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ResourceInfoResponse response = resourceService.getResourceInfo(path, userDetails.user().getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/resource", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload files",
            description = "Uploads one or multiple files to the specified storage path"
    )
    @ApiResponse(responseCode = "201", description = "Files uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid path or empty files",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Parent directory not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Resource with this name already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<ResourceInfoResponse>> uploadResource(@RequestParam String path,
                                                                     @RequestParam("object") List<MultipartFile> files,
                                                                     @AuthenticationPrincipal UserDetailsImpl userDetails) {

        List<ResourceInfoResponse> responses = resourceService.upload(files, path, userDetails.user().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping("/resource/download")
    @Operation(
            summary = "Download resource",
            description = "Downloads a file or a directory by path"
    )
    @ApiResponse(responseCode = "200", description = "Downloads a file or a directory as ZIP archive by path",
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE))
    @ApiResponse(responseCode = "400", description = "Invalid path",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Resource not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void downloadResource(@RequestParam String path,
                                 HttpServletResponse response,
                                 @AuthenticationPrincipal UserDetailsImpl userDetails) throws IOException {

        Long userId = userDetails.user().getId();
        ResourceInfoResponse resourceInfo = resourceService.getResourceInfo(path, userId);

        String fileName = resourceInfo.name();
        if (resourceInfo.type() == ResourceType.DIRECTORY) {
            fileName += ".zip";
        } else {
            response.setContentLengthLong(resourceInfo.size());
        }

        ContentDisposition contentDisposition = ContentDisposition
                .attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();

        response.setContentType(MediaType.APPLICATION_OCTET_STREAM.toString());
        response.setStatus(HttpStatus.OK.value());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        resourceService.download(path, userDetails.user().getId(), response.getOutputStream());
    }

    @DeleteMapping("/resource")
    @Operation(
            summary = "Delete resource",
            description = "Deletes a file or directory by path"
    )
    @ApiResponse(responseCode = "204", description = "Resource deleted successfully")
    @ApiResponse(responseCode = "400", description = "Invalid path",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Resource not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> deleteResource(@RequestParam String path,
                                               @AuthenticationPrincipal UserDetailsImpl userDetails) {
        resourceService.delete(path, userDetails.user().getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resource/move")
    @Operation(
            summary = "Move or rename resource",
            description = "Moves or renames a file or directory"
    )
    @ApiResponse(responseCode = "200", description = "Resource moved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid path",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Source resource not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Destination path already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ResourceInfoResponse> moveResource(@RequestParam String from,
                                                             @RequestParam String to,
                                                             @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ResourceInfoResponse response = resourceService.moveResource(from, to, userDetails.user().getId());
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/directory")
    @Operation(
            summary = "Get directory content",
            description = "Retrieves a list of files and subdirectories inside the specified directory"
    )
    @ApiResponse(responseCode = "200", description = "Directory content retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid path",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Directory not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<ResourceInfoResponse>> getDirectoryContent(@RequestParam String path,
                                                                          @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<ResourceInfoResponse> responses = resourceService.getDirectoryContentInfo(path, userDetails.user().getId());
        return ResponseEntity.ok().body(responses);
    }

    @PostMapping("/directory")
    @Operation(
            summary = "Create empty directory",
            description = "Creates a new empty directory at the specified path"
    )
    @ApiResponse(responseCode = "201", description = "Directory created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid path",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Directory already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ResourceInfoResponse> createEmptyDirectory(@RequestParam String path,
                                                                     @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ResourceInfoResponse response = resourceService.createEmptyDirectory(path, userDetails.user().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/resource/search")
    @Operation(
            summary = "Search resources",
            description = "Searches for resources matching the query parameter"
    )
    @ApiResponse(responseCode = "200", description = "Search completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid query or validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<ResourceInfoResponse>> searchResources(@Valid SearchRequest searchRequest,
                                                                      @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<ResourceInfoResponse> searchResults = resourceService.searchResource(
                searchRequest.query(), userDetails.user().getId());
        return ResponseEntity.ok().body(searchResults);
    }
}