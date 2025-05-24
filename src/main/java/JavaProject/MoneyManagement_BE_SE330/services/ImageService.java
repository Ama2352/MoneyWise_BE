package JavaProject.MoneyManagement_BE_SE330.services;

import org.springframework.web.multipart.MultipartFile;

public interface ImageService {
    String uploadImage(MultipartFile file, Long userId) throws Exception;
    void deleteImage(String imageUrl) throws Exception;
}