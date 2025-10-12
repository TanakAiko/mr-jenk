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

        // Remove file extension first
        String nameWithoutExtension = originalFileName;
        String extension = "";
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            nameWithoutExtension = originalFileName.substring(0, lastDotIndex);
            extension = originalFileName.substring(lastDotIndex);
        }

        // Remove emojis and special characters, keep only alphanumeric, dots, hyphens, and underscores
        String sanitized = nameWithoutExtension.replaceAll("[^a-zA-Z0-9._-]", "");
        
        // Remove multiple consecutive dots, hyphens, or underscores
        sanitized = sanitized.replaceAll("[._-]+", "_");
        
        // Remove leading/trailing dots, hyphens, or underscores
        sanitized = sanitized.replaceAll("^[._-]+|[._-]+$", "");
        
        // If sanitized name is empty, use "file"
        if (sanitized.isEmpty()) {
            sanitized = "file";
        }

        // Limit length to 50 characters (excluding extension)
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }

        return sanitized + extension;
    }
}
