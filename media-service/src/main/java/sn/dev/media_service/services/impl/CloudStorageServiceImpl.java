package sn.dev.media_service.services.impl;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import sn.dev.media_service.services.CloudStorageService;

@Service
public class CloudStorageServiceImpl implements CloudStorageService {

    @Value("${supabase.project-url}")
    private String projectUrl;

    @Value("${supabase.api-key}")
    private String apiKey;

    @Value("${supabase.bucket-name}")
    private String bucketName;

    private RestTemplate restTemplate = new RestTemplate();

    @Override
    public String upload(MultipartFile file) {
        try {
            // Generate a unique file name using UUID and the sanitized original file name
            String sanitizedFileName = sanitizeFileName(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + "_" + sanitizedFileName;

            // Create headers with the API key and content type
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            String contentType = file.getContentType();
            if (contentType == null) {
                throw new IllegalArgumentException("Missing content type on uploaded file");
            }
            headers.setContentType(MediaType.valueOf(contentType));

            // Construct the upload URL
            String uploadUrl = String.format("%s/storage/v1/object/%s/%s", projectUrl, bucketName, fileName);

            // Create the request entity with the file bytes and headers
            HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);

            // Send the PUT request to upload the file
            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.PUT,
                    requestEntity,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return String.format("%s/storage/v1/object/public/%s/%s", projectUrl, bucketName, fileName);
            } else {
                throw new RuntimeException("Failed to upload file: " + response.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to cloud storage", e);
        }
    }

    /**
     * Sanitizes a filename by removing special characters, emojis, and spaces
     * that are not allowed in Supabase Storage keys
     */
    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            return "file";
        }

        FileNameParts parts = splitFileName(originalFileName);

        String baseName = removeDisallowedCharacters(parts.baseName);
        baseName = normalizeSeparators(baseName);
        baseName = ensureNonEmpty(baseName);
        baseName = limitLength(baseName, 50);

        return baseName + parts.extension;
    }

    /** Simple holder for base name and extension parts. */
    private static class FileNameParts {
        final String baseName;
        final String extension;

        FileNameParts(String baseName, String extension) {
            this.baseName = baseName;
            this.extension = extension;
        }
    }

    private FileNameParts splitFileName(String originalFileName) {
        String nameWithoutExtension = originalFileName;
        String extension = "";
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            nameWithoutExtension = originalFileName.substring(0, lastDotIndex);
            extension = originalFileName.substring(lastDotIndex);
        }
        return new FileNameParts(nameWithoutExtension, extension);
    }

    private String removeDisallowedCharacters(String input) {
        // Keep only alphanumeric, dots, hyphens, and underscores
        return input.replaceAll("[^a-zA-Z0-9._-]", "");
    }

    private String normalizeSeparators(String input) {
        // Collapse sequences of '.', '_' or '-' to a single '_', and trim them from both ends
        StringBuilder sb = new StringBuilder();
        char lastOut = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            boolean isSeparator = (c == '.' || c == '_' || c == '-');

            if (isSeparator) {
                // Avoid leading separator and multiple separators
                if (sb.length() > 0 && lastOut != '_') {
                    sb.append('_');
                    lastOut = '_';
                }
            } else {
                sb.append(c);
                lastOut = c;
            }
        }

        // Remove trailing '_' if any
        int len = sb.length();
        if (len > 0 && sb.charAt(len - 1) == '_') {
            sb.setLength(len - 1);
        }

        return sb.toString();
    }

    private String ensureNonEmpty(String input) {
        return input.isEmpty() ? "file" : input;
    }

    private String limitLength(String input, int maxLength) {
        return input.length() > maxLength ? input.substring(0, maxLength) : input;
    }
}
