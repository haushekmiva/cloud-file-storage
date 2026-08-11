package com.haushekmiva.cloudfilestorage.service;

import com.haushekmiva.cloudfilestorage.TestcontainersConfiguration;
import com.haushekmiva.cloudfilestorage.dto.ResourceInfoResponse;
import com.haushekmiva.cloudfilestorage.dto.ResourceType;
import com.haushekmiva.cloudfilestorage.exception.InvalidPathException;
import com.haushekmiva.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.haushekmiva.cloudfilestorage.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@RequiredArgsConstructor
public class ResourceServiceIntegrationTest {

    private final ResourceService resourceService;
    private final FileStorageService fileStorageService;

    private static final Long TEST_USER1_ID = 777L;
    private static final Long TEST_USER2_ID = 778L;

    @DynamicPropertySource
    static void registerMinioProperties(DynamicPropertyRegistry registry) {
        registry.add("minio.endpoint", () -> "http://" + TestcontainersConfiguration.minioContainer.getHost()
                + ":" + TestcontainersConfiguration.minioContainer.getMappedPort(9000));
        registry.add("minio.access-key", () -> TestcontainersConfiguration.MINIO_ROOT_USER);
        registry.add("minio.secret-key", () -> TestcontainersConfiguration.MINIO_ROOT_PASSWORD);
    }

    @BeforeEach
    void cleanData() {
        fileStorageService.deleteObjects("user-" + TEST_USER1_ID + "-files/");
        fileStorageService.deleteObjects("user-" + TEST_USER2_ID + "-files/");
    }

    @Test
    void upload_savesInStorage() {
        MultipartFile file = createMultipartTextFile("passwords.txt", 10);

        resourceService.upload(List.of(file), "docs/", TEST_USER1_ID);
        ResourceInfoResponse response = resourceService.getResourceInfo("docs/passwords.txt", TEST_USER1_ID);

        assertThat(response).isEqualTo(new ResourceInfoResponse("docs/", "passwords.txt", ResourceType.FILE, 10L));
    }

    @Test
    void upload_toRootDirectory_savesToStorage() {
        MultipartFile file = createMultipartTextFile("passwords.txt", 10);

        resourceService.upload(List.of(file), "", TEST_USER1_ID);
        ResourceInfoResponse response = resourceService.getResourceInfo("passwords.txt", TEST_USER1_ID);
        assertThat(response).isEqualTo(new ResourceInfoResponse("", "passwords.txt", ResourceType.FILE, 10L));
    }

    @Test
    void upload_directory_savesToStorage() {
        List<MultipartFile> files = new ArrayList<>();

        files.add(createMultipartImageFile("my-folder/photo.jpg", 13));
        files.add(createMultipartImageFile("my-folder/documents/report-image.jpeg", 14));
        files.add(createMultipartTextFile("my-folder/documents/notes.txt", 9));

        resourceService.upload(files, "", TEST_USER1_ID);

        List<ResourceInfoResponse> uploadedDirectoryResponse = resourceService.getDirectoryContentInfo("my-folder/", TEST_USER1_ID);
        assertThat(uploadedDirectoryResponse).containsExactlyInAnyOrder(
                new ResourceInfoResponse("my-folder/", "documents", ResourceType.DIRECTORY),
                new ResourceInfoResponse("my-folder/", "photo.jpg", ResourceType.FILE, 13L)
        );

        List<ResourceInfoResponse> innerDirectoryResponse = resourceService.getDirectoryContentInfo(
                "my-folder/documents/", TEST_USER1_ID
        );
        assertThat(innerDirectoryResponse).containsExactlyInAnyOrder(
                new ResourceInfoResponse("my-folder/documents/", "report-image.jpeg",
                        ResourceType.FILE, 14L),
                new ResourceInfoResponse("my-folder/documents/", "notes.txt",
                        ResourceType.FILE, 9L)
        );
    }

    @Test
    void upload_incorrectPath_throwsInvalidPathException() {
        MultipartFile file = createMultipartTextFile("password.txt", 8);
        assertThatThrownBy(() -> resourceService.upload(List.of(file), "//docs", TEST_USER1_ID))
                .isInstanceOf(InvalidPathException.class);
    }

    @Test
    void upload_resourceAlreadyExists_throwsResourceAlreadyExistsException() {
        MultipartFile file = createMultipartImageFile("my-folder/photo.jpg", 13);

        resourceService.upload(List.of(file), "", TEST_USER1_ID);

        assertThatThrownBy(() -> resourceService.upload(List.of(file), "", TEST_USER1_ID))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void delete_file_removesObjectFromStorage() {
        MultipartFile file = createMultipartTextFile("passwords.txt", 8);

        resourceService.upload(List.of(file), "", TEST_USER1_ID);
        resourceService.delete("passwords.txt", TEST_USER1_ID);
        assertThatThrownBy(() -> resourceService.getResourceInfo("passwords.txt", TEST_USER1_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_rootDirectory_throwsInvalidPathException() {
        MultipartFile file = createMultipartImageFile("my-folder/photo.jpg", 12);

        resourceService.upload(List.of(file), "", TEST_USER1_ID);

        assertThatThrownBy(() -> resourceService.delete("", TEST_USER1_ID))
                .isInstanceOf(InvalidPathException.class);
    }

    @Test
    void delete_emptyDirectory_removesObject() {
        resourceService.createEmptyDirectory("docs/", TEST_USER1_ID);
        resourceService.delete("docs/", TEST_USER1_ID);
        assertThatThrownBy(() -> resourceService.getResourceInfo("docs/", TEST_USER1_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void move_file_movesToNewPath() {
        MultipartFile file = createMultipartTextFile("passwords.txt", 10);

        resourceService.upload(List.of(file), "docs/files/", TEST_USER1_ID);
        resourceService.moveResource("docs/files/passwords.txt", "docs/passwords.txt", TEST_USER1_ID);

        assertThatThrownBy(() -> resourceService.getResourceInfo("docs/files/passwords.txt", TEST_USER1_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        ResourceInfoResponse response = resourceService.getResourceInfo("docs/passwords.txt", TEST_USER1_ID);
        assertThat(response).isEqualTo(
                new ResourceInfoResponse("docs/", "passwords.txt", ResourceType.FILE, 10L)
        );
    }

    @Test
    void move_fileToAlreadyExistsPath_throwsResourceAlreadyExistsException() {
        List<MultipartFile> files = new ArrayList<>();

        files.add(createMultipartImageFile("photos/photo.jpg", 12));
        files.add(createMultipartImageFile("photo.jpg", 19));

        resourceService.upload(files, "", TEST_USER1_ID);
        assertThatThrownBy(() -> resourceService.moveResource("photos/photo.jpg", "photo.jpg", TEST_USER1_ID))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void moveDirectory_directoryMoves() {
        List<MultipartFile> files = new ArrayList<>();

        files.add(createMultipartImageFile("my-folder/photo.jpg", 13));
        files.add(createMultipartImageFile("my-folder/report-image.jpeg", 14));

        resourceService.upload(files, "", TEST_USER1_ID);

        resourceService.moveResource("my-folder/", "your-folder/my-folder/", TEST_USER1_ID);
        assertThat(resourceService.getDirectoryContentInfo("your-folder/my-folder/", TEST_USER1_ID)).containsExactlyInAnyOrder(
                new ResourceInfoResponse("your-folder/my-folder/", "photo.jpg", ResourceType.FILE, 13L),
                new ResourceInfoResponse("your-folder/my-folder/", "report-image.jpeg", ResourceType.FILE, 14L)
        );
        assertThat(resourceService.getDirectoryContentInfo("", TEST_USER1_ID)).containsExactly(
                new ResourceInfoResponse("", "your-folder/", ResourceType.DIRECTORY)
        );
    }

    @Test
    void moveDirectory_intoOwnDescendant_throwsInvalidPathException() {
        MultipartFile file = createMultipartTextFile("passwords.txt", 10);
        resourceService.upload(List.of(file), "a/docs/", TEST_USER1_ID);
        assertThatThrownBy(() -> resourceService.moveResource("a/docs/", "a/docs/sub/docs/", TEST_USER1_ID))
                .isInstanceOf(InvalidPathException.class);
    }

    @Test
    void moveFile_fromDirectory_directoryNotDisappears() {
        MultipartFile file = createMultipartTextFile("passwords.txt", 10);
        resourceService.upload(List.of(file), "a/docs/", TEST_USER1_ID);
        resourceService.moveResource("a/docs/passwords.txt", "a/passwords.txt", TEST_USER1_ID);
        assertThat(resourceService.getResourceInfo("a/docs/", TEST_USER1_ID)).isEqualTo(
                new ResourceInfoResponse("a/", "docs", ResourceType.DIRECTORY)
        );
    }

    @Test
    void search_findsNecessaryFiles() {
        List<MultipartFile> filesOfFirstUser = new ArrayList<>();

        filesOfFirstUser.add(createMultipartImageFile("my-folder/зима1999фотография.jpg", 3));
        filesOfFirstUser.add(createMultipartImageFile("my-folder/documents/фото_лето2006.pdf", 11));

        resourceService.upload(filesOfFirstUser, "", TEST_USER1_ID);
        List<ResourceInfoResponse> searchResults = resourceService.searchResource("фото", TEST_USER1_ID);

        assertThat(searchResults).containsExactlyInAnyOrder(
                new ResourceInfoResponse("my-folder/", "зима1999фотография.jpg", ResourceType.FILE, 3L),
                new ResourceInfoResponse("my-folder/documents/", "фото_лето2006.pdf", ResourceType.FILE, 11L)
        );
    }

    @Test
    void search_findsOnlyUsersFiles() {
        List<MultipartFile> filesOfFirstUser = new ArrayList<>();
        List<MultipartFile> filesOfSecondUser = new ArrayList<>();

        filesOfFirstUser.add(createMultipartImageFile("my-folder/photo.jpg", 3));
        filesOfFirstUser.add(createMultipartImageFile("my-folder/documents/report.pdf", 11));
        filesOfSecondUser.add(createMultipartTextFile("books.txt", 13));

        resourceService.upload(filesOfFirstUser, "", TEST_USER1_ID);
        resourceService.upload(filesOfSecondUser, "", TEST_USER2_ID);

        List<ResourceInfoResponse> searchResults = resourceService.searchResource("books.txt", TEST_USER1_ID);

        assertThat(searchResults).isEmpty();
    }

    @Test
    void createEmptyDirectory_appearsInDirectoryContent() {
        resourceService.createEmptyDirectory("Новая папка/", TEST_USER1_ID);

        assertThat(resourceService.getDirectoryContentInfo("", TEST_USER1_ID))
                .hasSize(1)
                .contains(new ResourceInfoResponse("", "Новая папка/", ResourceType.DIRECTORY));
    }

    @Test
    void createEmptyDirectory_throwsResourceAlreadyExists() {
        resourceService.createEmptyDirectory("docs/", TEST_USER1_ID);
        assertThatThrownBy(() -> resourceService.createEmptyDirectory("docs/", TEST_USER1_ID))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void createEmptyDirectory_thenUploadFile_doesNotDuplicateDirectory() {
        List<MultipartFile> files = new ArrayList<>();

        resourceService.createEmptyDirectory("documents/", TEST_USER1_ID);
        files.add(createMultipartImageFile("documents/report.pdf", 11));

        resourceService.upload(files, "", TEST_USER1_ID);

        assertThat(resourceService.getDirectoryContentInfo("", TEST_USER1_ID))
                .hasSize(1)
                .contains(new ResourceInfoResponse("", "documents/", ResourceType.DIRECTORY));

        List<ResourceInfoResponse> documentsContent = resourceService.getDirectoryContentInfo("documents/", TEST_USER1_ID);
        assertThat(documentsContent).containsExactly(
                new ResourceInfoResponse("documents/", "report.pdf", ResourceType.FILE, 11L)
        );
    }

    @Test
    void delete_directoryWithMarkerAndContent_removesEverything() {
        List<MultipartFile> files = new ArrayList<>();
        resourceService.createEmptyDirectory("documents/", TEST_USER1_ID);

        files.add(createMultipartImageFile("documents/report.pdf", 11));

        resourceService.upload(files, "", TEST_USER1_ID);
        resourceService.delete("documents/", TEST_USER1_ID);
        assertThatThrownBy(() -> resourceService.getResourceInfo("documents/", TEST_USER1_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private MultipartFile createMultipartTextFile(String name, int size) {
        byte[] content = "A".repeat(size).getBytes(StandardCharsets.UTF_8);

        return new MockMultipartFile(
                "files",
                name,
                "text/plain",
                content
        );
    }

    private MultipartFile createMultipartImageFile(String name, int size) {
        byte[] content = new byte[size];

        return new MockMultipartFile(
                "files",
                name,
                "image/jpeg",
                content
        );
    }
}