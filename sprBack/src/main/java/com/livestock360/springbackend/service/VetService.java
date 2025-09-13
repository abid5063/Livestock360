package com.livestock360.springbackend.service;

import com.google.gson.Gson;
import com.livestock360.springbackend.model.Vet;
import com.livestock360.springbackend.utils.PasswordUtil;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class VetService {

    private final MongoDatabase database;
    private final Gson gson;
    private final SimpleDateFormat dateFormat;

    @Autowired
    public VetService(MongoDatabase database) {
        this.database = database;
        this.gson = new Gson();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    private MongoCollection<Document> getVetsCollection() {
        return database.getCollection("vets");
    }

    public Vet findByEmail(String email) {
        try {
            MongoCollection<Document> collection = getVetsCollection();
            Document doc = collection.find(Filters.eq("email", email)).first();
            
            if (doc != null) {
                return documentToVet(doc);
            }
            return null;
        } catch (Exception e) {
            System.out.println("Error finding vet by email: " + e.getMessage());
            return null;
        }
    }

    public Vet findById(String id) {
        try {
            MongoCollection<Document> collection = getVetsCollection();
            Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
            
            if (doc != null) {
                return documentToVet(doc);
            }
            return null;
        } catch (Exception e) {
            System.out.println("Error finding vet by ID: " + e.getMessage());
            return null;
        }
    }

    public Vet save(Vet vet) {
        try {
            MongoCollection<Document> collection = getVetsCollection();
            Document doc = vetToDocument(vet);
            
            if (vet.getId() == null) {
                // Insert new vet
                collection.insertOne(doc);
                vet.setId(doc.getObjectId("_id"));
            } else {
                // Update existing vet
                collection.replaceOne(
                    Filters.eq("_id", vet.getId()),
                    doc
                );
            }
            
            return vet;
        } catch (Exception e) {
            System.out.println("Error saving vet: " + e.getMessage());
            return null;
        }
    }

    public boolean deleteById(String id) {
        try {
            MongoCollection<Document> collection = getVetsCollection();
            return collection.deleteOne(Filters.eq("_id", new ObjectId(id))).getDeletedCount() > 0;
        } catch (Exception e) {
            System.out.println("Error deleting vet: " + e.getMessage());
            return false;
        }
    }

    public List<Vet> getAllVets() {
        try {
            MongoCollection<Document> collection = getVetsCollection();
            List<Vet> vets = new ArrayList<>();
            
            collection.find().forEach(doc -> {
                Vet vet = documentToVet(doc);
                if (vet != null) {
                    vets.add(vet);
                }
            });
            
            return vets;
        } catch (Exception e) {
            System.out.println("Error getting all vets: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Vet createVet(String name, String email, String password, String phone,
                        String location, String address, String profilePicture,
                        String specialization, Double latitude, Double longitude) {
        try {
            // Check if vet already exists
            if (findByEmail(email) != null) {
                return null; // Vet already exists
            }

            // Generate salt and hash password
            String salt = PasswordUtil.generateSalt();
            String hashedPassword = PasswordUtil.hashPassword(password, salt);
            
            // Create vet with current timestamp
            String dateJoined = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            Vet vet = new Vet(name, email, hashedPassword, salt, phone, location, address,
                             profilePicture, dateJoined, specialization, latitude, longitude);
            
            return save(vet);
        } catch (Exception e) {
            System.out.println("Error creating vet: " + e.getMessage());
            return null;
        }
    }

    public boolean verifyPassword(Vet vet, String password) {
        return PasswordUtil.verifyPassword(password, vet.getPassword(), vet.getSalt());
    }

    public Vet createVet(Vet vet) {
        try {
            System.out.println("Creating new vet with name: " + vet.getName());
            
            // Check if vet already exists
            if (findByEmail(vet.getEmail()) != null) {
                System.out.println("Vet already exists with email: " + vet.getEmail());
                return null;
            }

            // Set timestamp
            if (vet.getDateJoined() == null) {
                vet.setDateJoined(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            
            System.out.println("Saving vet to database");
            Vet savedVet = save(vet);
            System.out.println("Vet saved successfully with ID: " + savedVet.getId());
            
            return savedVet;
        } catch (Exception e) {
            System.out.println("Error creating vet: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Vet updateVet(Vet vet) {
        try {
            System.out.println("Updating vet with ID: " + vet.getId());
            return save(vet);
        } catch (Exception e) {
            System.out.println("Error updating vet: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public List<Vet> findAll() {
        return getAllVets();
    }

    public List<Vet> findByLocation(String location) {
        try {
            MongoCollection<Document> collection = getVetsCollection();
            List<Vet> vets = new ArrayList<>();
            
            // Search for vets with location containing the search term (case insensitive)
            collection.find(Filters.regex("location", ".*" + location + ".*", "i")).forEach(doc -> {
                Vet vet = documentToVet(doc);
                if (vet != null) {
                    vets.add(vet);
                }
            });
            
            return vets;
        } catch (Exception e) {
            System.out.println("Error finding vets by location: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Vet findByLicenseNumber(String licenseNumber) {
        try {
            MongoCollection<Document> collection = getVetsCollection();
            Document doc = collection.find(Filters.eq("licenseNumber", licenseNumber)).first();
            
            if (doc != null) {
                return documentToVet(doc);
            }
            return null;
        } catch (Exception e) {
            System.out.println("Error finding vet by license number: " + e.getMessage());
            return null;
        }
    }

    public List<Vet> searchVets(String searchTerm, String location, String specialty) {
        try {
            MongoCollection<Document> collection = getVetsCollection();
            List<Vet> vets = new ArrayList<>();
            
            // Build MongoDB query exactly like SimpleBackend
            Document mongoQuery = new Document();
            
            // Handle specialty filter (exact match)
            if (specialty != null && !specialty.trim().isEmpty()) {
                mongoQuery.append("specialty", specialty);
            }
            
            // Build OR conditions for location and search
            List<Document> orConditions = new ArrayList<>();
            
            if (location != null && !location.trim().isEmpty()) {
                orConditions.add(new Document("location", new Document("$regex", location).append("$options", "i")));
            }
            
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                orConditions.add(new Document("name", new Document("$regex", searchTerm).append("$options", "i")));
                orConditions.add(new Document("specialty", new Document("$regex", searchTerm).append("$options", "i")));
                orConditions.add(new Document("bio", new Document("$regex", searchTerm).append("$options", "i")));
            }
            
            if (!orConditions.isEmpty()) {
                mongoQuery.append("$or", orConditions);
            }
            
            // Execute query with sorting and limit (exactly like SimpleBackend)
            org.bson.conversions.Bson sortOrder = new Document("rating", -1).append("totalReviews", -1);
            
            collection.find(mongoQuery)
                .sort(sortOrder)
                .limit(20)
                .forEach(doc -> {
                    Vet vet = documentToVet(doc);
                    if (vet != null) {
                        vets.add(vet);
                    }
                });
            
            return vets;
        } catch (Exception e) {
            System.out.println("Error searching vets: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void deleteVet(String id) {
        try {
            System.out.println("Deleting vet with ID: " + id);
            deleteById(id);
        } catch (Exception e) {
            System.out.println("Error deleting vet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Vet documentToVet(Document doc) {
        try {
            Vet vet = new Vet();
            vet.setId(doc.getObjectId("_id"));
            vet.setName(doc.getString("name"));
            vet.setEmail(doc.getString("email"));
            vet.setPassword(doc.getString("password"));
            vet.setSalt(doc.getString("salt"));
            vet.setPhone(doc.getString("phone"));
            vet.setPhoneNo(doc.getString("phoneNo"));
            vet.setLocation(doc.getString("location"));
            vet.setAddress(doc.getString("address"));
            vet.setProfilePicture(doc.getString("profilePicture"));
            vet.setProfileImage(doc.getString("profileImage"));
            
            // Handle dateJoined safely
            Object dateJoinedObj = doc.get("dateJoined");
            if (dateJoinedObj != null) {
                if (dateJoinedObj instanceof String) {
                    vet.setDateJoined((String) dateJoinedObj);
                } else if (dateJoinedObj instanceof java.util.Date) {
                    vet.setDateJoined(dateFormat.format((java.util.Date) dateJoinedObj));
                }
            }
            
            vet.setSpecialization(doc.getString("specialization"));
            vet.setSpecialty(doc.getString("specialty"));
            vet.setLicenseNumber(doc.getString("licenseNumber"));
            vet.setExperience(doc.getString("experience"));
            vet.setUserType(doc.getString("userType"));
            vet.setIsVerified(doc.getBoolean("isVerified"));
            vet.setIsActive(doc.getBoolean("isActive"));
            
            // Handle numeric fields safely
            Object ratingObj = doc.get("rating");
            if (ratingObj != null) {
                if (ratingObj instanceof Double) {
                    vet.setRating((Double) ratingObj);
                } else if (ratingObj instanceof Integer) {
                    vet.setRating(((Integer) ratingObj).doubleValue());
                } else if (ratingObj instanceof Number) {
                    vet.setRating(((Number) ratingObj).doubleValue());
                }
            }
            
            vet.setTotalReviews(doc.getInteger("totalReviews"));
            
            // Handle date fields safely
            Object regDateObj = doc.get("registrationDate");
            if (regDateObj != null) {
                if (regDateObj instanceof String) {
                    vet.setRegistrationDate((String) regDateObj);
                } else if (regDateObj instanceof java.util.Date) {
                    vet.setRegistrationDate(dateFormat.format((java.util.Date) regDateObj));
                }
            }
            
            Object createdAtObj = doc.get("createdAt");
            if (createdAtObj != null) {
                if (createdAtObj instanceof String) {
                    vet.setCreatedAt((String) createdAtObj);
                } else if (createdAtObj instanceof java.util.Date) {
                    vet.setCreatedAt(dateFormat.format((java.util.Date) createdAtObj));
                }
            }
            
            Object updatedAtObj = doc.get("updatedAt");
            if (updatedAtObj != null) {
                if (updatedAtObj instanceof String) {
                    vet.setUpdatedAt((String) updatedAtObj);
                } else if (updatedAtObj instanceof java.util.Date) {
                    vet.setUpdatedAt(dateFormat.format((java.util.Date) updatedAtObj));
                }
            }
            
            vet.setTotalAppointments(doc.getInteger("totalAppointments"));
            vet.setCompletedAppointments(doc.getInteger("completedAppointments"));
            vet.setCancelledAppointments(doc.getInteger("cancelledAppointments"));
            
            // Handle consultationFee safely
            Object feeObj = doc.get("consultationFee");
            if (feeObj != null) {
                if (feeObj instanceof Double) {
                    vet.setConsultationFee((Double) feeObj);
                } else if (feeObj instanceof Integer) {
                    vet.setConsultationFee(((Integer) feeObj).doubleValue());
                } else if (feeObj instanceof Number) {
                    vet.setConsultationFee(((Number) feeObj).doubleValue());
                }
            }
            
            vet.setBio(doc.getString("bio"));
            
            // Handle latitude/longitude safely
            Object latObj = doc.get("latitude");
            if (latObj != null) {
                if (latObj instanceof Double) {
                    vet.setLatitude((Double) latObj);
                } else if (latObj instanceof Number) {
                    vet.setLatitude(((Number) latObj).doubleValue());
                }
            }
            
            Object lonObj = doc.get("longitude");
            if (lonObj != null) {
                if (lonObj instanceof Double) {
                    vet.setLongitude((Double) lonObj);
                } else if (lonObj instanceof Number) {
                    vet.setLongitude(((Number) lonObj).doubleValue());
                }
            }
            return vet;
        } catch (Exception e) {
            System.out.println("Error converting document to vet: " + e.getMessage());
            return null;
        }
    }

    private Document vetToDocument(Vet vet) {
        Document doc = new Document();
        if (vet.getId() != null) {
            doc.append("_id", vet.getId());
        }
        doc.append("name", vet.getName())
           .append("email", vet.getEmail())
           .append("password", vet.getPassword())
           .append("salt", vet.getSalt())
           .append("phone", vet.getPhone())
           .append("phoneNo", vet.getPhoneNo())
           .append("location", vet.getLocation())
           .append("address", vet.getAddress())
           .append("profilePicture", vet.getProfilePicture())
           .append("profileImage", vet.getProfileImage())
           .append("dateJoined", vet.getDateJoined())
           .append("specialization", vet.getSpecialization())
           .append("specialty", vet.getSpecialty())
           .append("licenseNumber", vet.getLicenseNumber())
           .append("experience", vet.getExperience())
           .append("userType", vet.getUserType())
           .append("isVerified", vet.getIsVerified())
           .append("isActive", vet.getIsActive())
           .append("rating", vet.getRating())
           .append("totalReviews", vet.getTotalReviews())
           .append("registrationDate", vet.getRegistrationDate())
           .append("createdAt", vet.getCreatedAt())
           .append("updatedAt", vet.getUpdatedAt())
           .append("totalAppointments", vet.getTotalAppointments())
           .append("completedAppointments", vet.getCompletedAppointments())
           .append("cancelledAppointments", vet.getCancelledAppointments())
           .append("consultationFee", vet.getConsultationFee())
           .append("bio", vet.getBio())
           .append("latitude", vet.getLatitude())
           .append("longitude", vet.getLongitude());
        return doc;
    }
}
