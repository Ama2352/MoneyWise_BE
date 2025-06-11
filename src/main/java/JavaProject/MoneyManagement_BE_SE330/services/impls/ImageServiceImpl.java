package JavaProject.MoneyManagement_BE_SE330.services.impls;

import JavaProject.MoneyManagement_BE_SE330.services.ImageService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageServiceImpl implements ImageService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/gif");

    @Autowired
    private Cloudinary cloudinary;

    @Value("${cloudinary.upload-preset}")
    private String uploadPreset;    
    
    @Override
    public String uploadImage(MultipartFile file, UUID userId) throws Exception {
        // Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be empty");
        }

        // Enhanced validation for Android compatibility
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        
        // Debug logging to understand what Android is sending
        System.out.println("=== Image Upload Debug ===");
        System.out.println("Content-Type: " + contentType);
        System.out.println("Original filename: " + originalFilename);
        System.out.println("File size: " + file.getSize());
          // Enhanced validation that works with Android
        if (!isValidImageType(contentType, originalFilename, file.getBytes())) {
            throw new IllegalArgumentException(
                String.format("Unsupported file type. Received Content-Type: '%s', Filename: '%s'. Allowed types: JPEG, PNG, GIF (including .tmp files with valid image content)", 
                    contentType, originalFilename)
            );
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Image size exceeds 10 MB limit");
        }        try {
            // Upload to Cloudinary with user-specific folder
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "upload_preset", uploadPreset,
                    "folder", "user_avatars/" + userId // Organize by user ID
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(), uploadParams);
            return (String) result.get("secure_url"); // Use secure_url for HTTPS
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image to Cloudinary", e);
        }
    }

    @Override
    public void deleteImage(String imageUrl) throws Exception {
        try {
            // Extract public ID from URL
            String publicId = extractPublicId(imageUrl);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete image from Cloudinary", e);
        }
    }

    private String extractPublicId(String imageUrl) {
        // Example URL: https://res.cloudinary.com/<cloud_name>/image/upload/v1234567890/user_avatars/123/image.jpg
        String[] parts = imageUrl.split("/");
        String fileName = parts[parts.length - 1];
        return parts[parts.length - 2] + "/" + fileName.split("\\.")[0]; // e.g., user_avatars/123/image
    }

    // Enhanced validation methods for Android compatibility
    private boolean isValidImageType(String contentType, String filename, byte[] fileBytes) {
        System.out.println("=== Validation Debug ===");
        System.out.println("Content-Type: '" + contentType + "'");
        System.out.println("Filename: '" + filename + "'");
        System.out.println("File bytes length: " + (fileBytes != null ? fileBytes.length : "null"));
        if (fileBytes != null && fileBytes.length >= 4) {
            System.out.printf("First 4 bytes: %02X %02X %02X %02X%n", 
                fileBytes[0] & 0xFF, fileBytes[1] & 0xFF, fileBytes[2] & 0xFF, fileBytes[3] & 0xFF);
        }
        
        // Method 1: Check by Content-Type (primary method)
        if (contentType != null) {
            String normalizedContentType = contentType.toLowerCase().trim();
            if (ALLOWED_TYPES.contains(normalizedContentType) || 
                normalizedContentType.equals("image/jpg") || // Android sometimes sends image/jpg
                normalizedContentType.equals("image/*")) { // Android sometimes sends generic image/*
                System.out.println("Content-Type validation: PASSED");
                return true;
            }
        }
          // Method 2: Check by file extension (fallback for Android)
        if (filename != null && !filename.trim().isEmpty()) {
            String extension = getFileExtension(filename).toLowerCase();
            if (extension.equals("jpg") || extension.equals("jpeg") || 
                extension.equals("png") || extension.equals("gif")) {
                return true;
            }
            // Accept .tmp files from Android (will be validated by file signature)
            if (extension.equals("tmp")) {
                return isValidImageBySignature(fileBytes);
            }
        }
        
        // Method 3: Check by file signature/magic bytes (most reliable)
        return isValidImageBySignature(fileBytes);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex != -1 && lastDotIndex < filename.length() - 1 ? 
               filename.substring(lastDotIndex + 1) : "";
    }    private boolean isValidImageBySignature(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length < 4) {
            System.out.println("File signature validation: FAILED - insufficient bytes");
            return false;
        }
        
        System.out.println("=== File Signature Check ===");
        System.out.printf("Checking signature with bytes: %02X %02X %02X %02X...%n", 
            fileBytes[0] & 0xFF, fileBytes[1] & 0xFF, fileBytes[2] & 0xFF, fileBytes[3] & 0xFF);
        
        // Check JPEG signature (FF D8)
        if (fileBytes.length >= 2 && 
            (fileBytes[0] & 0xFF) == 0xFF && (fileBytes[1] & 0xFF) == 0xD8) {
            System.out.println("File signature validation: PASSED - JPEG detected");
            return true; // JPEG
        }
        
        // Check PNG signature (89 50 4E 47)
        if (fileBytes.length >= 8 && 
            (fileBytes[0] & 0xFF) == 0x89 && fileBytes[1] == 0x50 && 
            fileBytes[2] == 0x4E && fileBytes[3] == 0x47) {
            System.out.println("File signature validation: PASSED - PNG detected");
            return true; // PNG
        }
        
        // Check GIF signature (47 49 46)
        if (fileBytes.length >= 6 && 
            fileBytes[0] == 0x47 && fileBytes[1] == 0x49 && fileBytes[2] == 0x46) {
            System.out.println("File signature validation: PASSED - GIF detected");
            return true; // GIF
        }
        
        System.out.println("File signature validation: FAILED - no valid image signature found");
        return false;
    }
}