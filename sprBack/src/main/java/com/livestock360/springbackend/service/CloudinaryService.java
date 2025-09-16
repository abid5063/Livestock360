package com.livestock360.springbackend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * Cloudinary service class - replicating Node.js cloudinary upload functionality
 * Handles image upload and deletion operations exactly like the Node.js backend
 */
@Service
public class CloudinaryService {
    
    @Autowired
    private Cloudinary cloudinary;
    
    /**F
     * Upload image to Cloudinary
     * Replicates: const uploadResponse = await cloudinary.uploader.upload(image, {folder: "animals", resource_type: "auto"});
     * 
     * @param image Base64 encoded image string or image data
     * @return The secure URL of the uploaded image
     * @throws IOException if upload fails
     */
    public String uploadImage(String image) throws IOException {
        if (image == null || image.trim().isEmpty()) {
            return "";
        }
        
        System.out.println("typeof image: " + image.getClass().getSimpleName());
        System.out.println("image starts with: " + (image.length() > 30 ? image.substring(0, 30) : image));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> uploadParams = ObjectUtils.asMap(
            "folder", "animals",
            "resource_type", "auto"
        );
        
        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResponse = cloudinary.uploader().upload(image, uploadParams);
        return (String) uploadResponse.get("secure_url");
    }
    
    /**
     * Delete image from Cloudinary
     * Replicates: await cloudinary.uploader.destroy(publicId);
     * 
     * @param imageUrl The full Cloudinary URL of the image to delete
     * @throws IOException if deletion fails
     */
    public void deleteImage(String imageUrl) throws IOException {
        if (imageUrl == null || !imageUrl.contains("cloudinary")) {
            return;
        }
        
        try {
            // Extract public ID from URL - replicating Node.js logic
            String[] parts = imageUrl.split("/");
            String fileWithExt = parts[parts.length - 1];
            String folder = parts[parts.length - 2];
            String publicId = folder + "/" + fileWithExt.split("\\.")[0];
            
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception deleteError) {
            System.out.println("Error deleting image from cloudinary: " + deleteError.getMessage());
        }
    }
}