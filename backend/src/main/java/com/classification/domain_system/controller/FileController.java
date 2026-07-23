package com.classification.domain_system.controller;

import com.classification.domain_system.exception.BusinessException;
import com.classification.domain_system.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final Path fileStorageLocation;

    public FileController(@Value("${file.upload-dir:./uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.UPLOAD_DIR_FAIL, "Could not create the directory where uploaded files will be stored: " + uploadDir);
        }
    }

    private String calculateHash(MultipartFile file) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = file.getInputStream()) {
            byte[] bytes = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(bytes)) != -1) {
                digest.update(bytes, 0, bytesRead);
            }
        }
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            originalFileName = "unknown_file";
        }

        try {
            String hash = calculateHash(file);
            String extension = getFileExtension(originalFileName);
            String savedFileName = hash + extension;

            Path targetLocation = this.fileStorageLocation.resolve(savedFileName);
            
            // 중복 파일 체크: 해시값이 같으면 이미 존재하는 파일이므로 덮어쓰거나 무시
            if (!Files.exists(targetLocation)) {
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            // 원본 파일명을 URL 파라미터로 인코딩하여 포함
            String encodedOriginalName = UriUtils.encode(originalFileName, StandardCharsets.UTF_8);
            String fileDownloadUri = "/api/files/download/" + savedFileName + "?name=" + encodedOriginalName;

            Map<String, String> response = new HashMap<>();
            response.put("fileName", originalFileName);
            response.put("url", fileDownloadUri);

            return ResponseEntity.ok(response);
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new BusinessException(ErrorCode.UPLOAD_FILE_FAIL, "Could not store file " + originalFileName + ". Please try again!");
        }
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, @RequestParam(value = "name", required = false) String originalName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                String downloadName = originalName != null ? originalName : resource.getFilename();
                String safeName = UriUtils.encode(downloadName, StandardCharsets.UTF_8);
                
                ContentDisposition contentDisposition = ContentDisposition.attachment()
                        .filename(safeName)
                        .filename(downloadName, StandardCharsets.UTF_8)
                        .build();

                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
