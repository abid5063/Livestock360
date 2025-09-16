package com.livestock360.springbackend.service;

import com.livestock360.springbackend.model.Vaccine;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class VaccineService {

    private final MongoDatabase database;

    @Autowired
    public VaccineService(MongoDatabase database) {
        this.database = database;
    }

    private MongoCollection<Document> getVaccinesCollection() {
        return database.getCollection("vaccines");
    }

    public Vaccine save(Vaccine vaccine) {
        try {
            MongoCollection<Document> collection = getVaccinesCollection();
            Document doc = vaccineToDocument(vaccine);
            
            if (vaccine.getId() == null) {
                // Insert new vaccine
                collection.insertOne(doc);
                vaccine.setId(doc.getObjectId("_id"));
            } else {
                // Update existing vaccine
                collection.replaceOne(
                    Filters.eq("_id", vaccine.getId()),
                    doc
                );
            }
            
            return vaccine;
        } catch (Exception e) {
            System.out.println("Error saving vaccine: " + e.getMessage());
            return null;
        }
    }

    public Vaccine findById(String id, String farmerId) {
        try {
            MongoCollection<Document> collection = getVaccinesCollection();
            Document doc = collection.find(Filters.and(
                Filters.eq("_id", new ObjectId(id)),
                Filters.eq("farmer", new ObjectId(farmerId))
            )).first();
            
            if (doc != null) {
                return documentToVaccine(doc);
            }
            return null;
        } catch (Exception e) {
            System.out.println("Error finding vaccine by ID: " + e.getMessage());
            return null;
        }
    }

    public List<Vaccine> findByFarmerId(String farmerId) {
        try {
            MongoCollection<Document> collection = getVaccinesCollection();
            List<Vaccine> vaccines = new ArrayList<>();
            
            collection.find(Filters.eq("farmer", new ObjectId(farmerId)))
                .sort(new Document("vaccine_date", -1))
                .forEach(doc -> {
                    Vaccine vaccine = documentToVaccine(doc);
                    if (vaccine != null) {
                        vaccines.add(vaccine);
                    }
                });
                
            return vaccines;
        } catch (Exception e) {
            System.out.println("Error finding vaccines by farmer ID: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Vaccine> findByAnimalId(String animalId, String farmerId) {
        try {
            MongoCollection<Document> collection = getVaccinesCollection();
            List<Vaccine> vaccines = new ArrayList<>();
            
            collection.find(Filters.and(
                Filters.eq("animal", new ObjectId(animalId)),
                Filters.eq("farmer", new ObjectId(farmerId))
            ))
            .sort(new Document("vaccine_date", -1))
            .forEach(doc -> {
                Vaccine vaccine = documentToVaccine(doc);
                if (vaccine != null) {
                    vaccines.add(vaccine);
                }
            });
                
            return vaccines;
        } catch (Exception e) {
            System.out.println("Error finding vaccines by animal ID: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean deleteById(String id, String farmerId) {
        try {
            MongoCollection<Document> collection = getVaccinesCollection();
            return collection.deleteOne(Filters.and(
                Filters.eq("_id", new ObjectId(id)),
                Filters.eq("farmer", new ObjectId(farmerId))
            )).getDeletedCount() > 0;
        } catch (Exception e) {
            System.out.println("Error deleting vaccine: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteByAnimalAndFarmer(String animalId, String farmerId) {
        try {
            MongoCollection<Document> collection = getVaccinesCollection();
            return collection.deleteMany(Filters.and(
                Filters.eq("animal", new ObjectId(animalId)),
                Filters.eq("farmer", new ObjectId(farmerId))
            )).getDeletedCount() >= 0;
        } catch (Exception e) {
            System.out.println("Error deleting vaccines for animal: " + e.getMessage());
            return false;
        }
    }

    public Vaccine updateVaccine(Vaccine vaccine) {
        try {
            vaccine.setUpdatedAt(new Date());
            return save(vaccine);
        } catch (Exception e) {
            System.out.println("Error updating vaccine: " + e.getMessage());
            return null;
        }
    }

    private Vaccine documentToVaccine(Document doc) {
        try {
            Vaccine vaccine = new Vaccine();
            vaccine.setId(doc.getObjectId("_id"));
            vaccine.setVaccineName(doc.getString("vaccine_name"));
            vaccine.setAnimal(doc.getObjectId("animal"));
            vaccine.setAnimalName(doc.getString("animal_name"));
            vaccine.setVaccineDate(doc.getDate("vaccine_date"));
            vaccine.setNotes(doc.getString("notes"));
            vaccine.setNextDueDate(doc.getDate("next_due_date"));
            vaccine.setFarmer(doc.getObjectId("farmer"));
            vaccine.setCreatedAt(doc.getDate("createdAt"));
            vaccine.setUpdatedAt(doc.getDate("updatedAt"));
            return vaccine;
        } catch (Exception e) {
            System.out.println("Error converting document to vaccine: " + e.getMessage());
            return null;
        }
    }

    private Document vaccineToDocument(Vaccine vaccine) {
        Document doc = new Document();
        if (vaccine.getId() != null) {
            doc.append("_id", vaccine.getId());
        }
        doc.append("vaccine_name", vaccine.getVaccineName())
           .append("animal", vaccine.getAnimal())
           .append("animal_name", vaccine.getAnimalName())
           .append("vaccine_date", vaccine.getVaccineDate())
           .append("notes", vaccine.getNotes())
           .append("next_due_date", vaccine.getNextDueDate())
           .append("farmer", vaccine.getFarmer())
           .append("createdAt", vaccine.getCreatedAt())
           .append("updatedAt", vaccine.getUpdatedAt());
        return doc;
    }
}