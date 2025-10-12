package sn.dev.media_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import sn.dev.media_service.data.entities.Media;
import sn.dev.media_service.data.repos.MediaRepo;
import sn.dev.media_service.services.CloudStorageService;
import sn.dev.media_service.services.impl.MediaServiceImpl;

@ExtendWith(MockitoExtension.class)
public class MediaServiceImplTest {
    @Mock
    private MediaRepo mediaRepo;

    @Mock
    private CloudStorageService cloudStorageService;

    @InjectMocks
    private MediaServiceImpl mediaService;

    private static byte[] pngBytes() {
        // Minimal valid PNG signature (8 bytes) + a little payload
        return new byte[] {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 'D', 'A', 'T', 'A'};
    }

    private static byte[] jpegBytes() {
        // Minimal JPEG SOI signature + payload
        return new byte[] {(byte)0xFF, (byte)0xD8, 'J', 'P', 'E', 'G'};
    }

    @Test
    void uploadAndSave_happyPath_savesMediaWithReturnedUrl() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "valid.png", "image/png", pngBytes()
        );
        when(cloudStorageService.upload(any())).thenReturn("https://cdn/valid.png");

        Media toSave = new Media();
        toSave.setImageUrl("https://cdn/valid.png");
        toSave.setProductId("p1");
        Media saved = new Media();
        saved.setId("m1");
        saved.setImageUrl("https://cdn/valid.png");
        saved.setProductId("p1");
        when(mediaRepo.save(any(Media.class))).thenReturn(saved);

        Media result = mediaService.uploadAndSave(file, "p1");

        assertThat(result.getId()).isEqualTo("m1");
        assertThat(result.getImageUrl()).isEqualTo("https://cdn/valid.png");
        assertThat(result.getProductId()).isEqualTo("p1");

        verify(cloudStorageService, times(1)).upload(file);
        verify(mediaRepo, times(1)).save(any(Media.class));
        System.out.println("✅ MEDIA/SERVICE: uploadAndSave_happyPath_savesMediaWithReturnedUrl() passed successfully.");
    }

    @Test
    void uploadAndSave_rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile(
            "file", "empty.png", "image/png", new byte[]{}
        );
        assertThatThrownBy(() -> mediaService.uploadAndSave(empty, "p1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("File cannot be null or empty");
        System.out.println("✅ MEDIA/SERVICE: uploadAndSave_rejectsEmptyFile() passed successfully.");
    }

    @Test
    void uploadAndSave_rejectsInvalidFileName() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "../evil.png", "image/png", pngBytes()
        );
        assertThatThrownBy(() -> mediaService.uploadAndSave(file, "p1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("File name contains invalid characters");
        System.out.println("✅ MEDIA/SERVICE: uploadAndSave_rejectsInvalidFileName() passed successfully.");
    }

    @Test
    void uploadAndSave_rejectsWrongContentType() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "note.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8)
        );
        assertThatThrownBy(() -> mediaService.uploadAndSave(file, "p1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Only JPEG, PNG, GIF and WEBP images are allowed");
        System.out.println("✅ MEDIA/SERVICE: uploadAndSave_rejectsWrongContentType() passed successfully.");
    }

    @Test
    void uploadAndSave_rejectsSignatureMismatch() {
        // Declared as PNG but bytes do not match PNG signature
        MockMultipartFile file = new MockMultipartFile(
            "file", "bad.png", "image/png", jpegBytes()
        );
        assertThatThrownBy(() -> mediaService.uploadAndSave(file, "p1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("File content does not match PNG format");
        System.out.println("✅ MEDIA/SERVICE: uploadAndSave_rejectsSignatureMismatch() passed successfully.");
    }

    @Test
    void uploadImage_happyPath_returnsUrl() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "valid.jpeg", "image/jpeg", jpegBytes()
        );
        when(cloudStorageService.upload(any())).thenReturn("https://cdn/img.jpeg");
        String url = mediaService.uploadImage(file);
        assertThat(url).isEqualTo("https://cdn/img.jpeg");
        verify(cloudStorageService, times(1)).upload(file);
        System.out.println("✅ MEDIA/SERVICE: uploadImage_happyPath_returnsUrl() passed successfully.");
    }

    @Test
    void findById_found_returnsEntity() {
        Media m = new Media();
        m.setId("m1");
        when(mediaRepo.findById("m1")).thenReturn(Optional.of(m));
        Media result = mediaService.findById("m1");
        assertThat(result.getId()).isEqualTo("m1");
        System.out.println("✅ MEDIA/SERVICE: findById_found_returnsEntity() passed successfully.");
    }

    @Test
    void findById_notFound_throws404() {
        when(mediaRepo.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> mediaService.findById("missing"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Media not found with id: missing");
        System.out.println("✅ MEDIA/SERVICE: findById_notFound_throws404() passed successfully.");
    }

    @Test
    void findByProductId_delegatesToRepo() {
        mediaService.findByProductId("p1");
        verify(mediaRepo, times(1)).findByProductId("p1");
        System.out.println("✅ MEDIA/SERVICE: findByProductId_delegatesToRepo() passed successfully.");
    }

    @Test
    void deleteById_delegatesToRepo() {
        mediaService.deleteById("m1");
        verify(mediaRepo, times(1)).deleteById("m1");
        System.out.println("✅ MEDIA/SERVICE: deleteById_delegatesToRepo() passed successfully.");
    }

    @Test
    void deleteByProductId_delegatesToRepo() {
        mediaService.deleteByProductId("p1");
        verify(mediaRepo, times(1)).deleteByProductId("p1");
        System.out.println("✅ MEDIA/SERVICE: deleteByProductId_delegatesToRepo() passed successfully.");
    }
}