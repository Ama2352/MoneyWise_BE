package JavaProject.MoneyManagement_BE_SE330.services;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ImageService {
    String uploadImage(MultipartFile file, UUID userId) throws Exception;
    void deleteImage(String imageUrl) throws Exception;
}