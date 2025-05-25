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
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Unsupported file type. Allowed types: JPEG, PNG, GIF");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Image size exceeds 10 MB limit");
        }

        try {
            // Upload to Cloudinary with user-specific folder
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "upload_preset", uploadPreset,
                    "folder", "user_avatars/" + userId // Organize by user ID
            );
            Map result = cloudinary.uploader().upload(file.getBytes(), uploadParams);
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
}