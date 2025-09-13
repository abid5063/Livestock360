package com.livestock360.springbackend.service;

import com.google.gson.Gson;
import com.livestock360.springbackend.model.Animal;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class AnimalService {

    private final MongoDatabase database;
    private final Gson gson;
    private final SimpleDateFormat dateFormat;

    @Autowired
    public AnimalService(MongoDatabase database) {
        this.database = database;
        this.gson = new Gson();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    private MongoCollection<Document> getAnimalsCollection() {
        return database.getCollection("animals");
    }

    public Animal findById(String id) {
        try {
            MongoCollection<Document> collection = getAnimalsCollection();
            Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
            
            if (doc != null) {
                return documentToAnimal(doc);
            }
            return null;
        } catch (Exception e) {
            System.out.println("Error finding animal by ID: " + e.getMessage());
            return null;
        }
    }

    public Animal findByFarmerAndId(String farmerId, String animalId) {
        try {
            MongoCollection<Document> collection = getAnimalsCollection();
            Document doc = collection.find(Filters.and(
                Filters.eq("_id", new ObjectId(animalId)),
                Filters.eq("farmer", new ObjectId(farmerId))
            )).first();
            
            if (doc != null) {
                return documentToAnimal(doc);
            }
            return null;
        } catch (Exception e) {
            System.out.println("Error finding animal by farmer and ID: " + e.getMessage());
            return null;
        }
    }

    public List<Animal> findByFarmerId(String farmerId) {
        try {
            MongoCollection<Document> collection = getAnimalsCollection();
            List<Animal> animals = new ArrayList<>();
            
            collection.find(Filters.eq("farmer", new ObjectId(farmerId)))
                    .sort(Sorts.descending("createdAt"))
                    .forEach(doc -> {
                        Animal animal = documentToAnimal(doc);
                        if (animal != null) {
                            animals.add(animal);
                        }
                    });
            
            return animals;
        } catch (Exception e) {
            System.out.println("Error finding animals by farmer ID: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Animal save(Animal animal) {
        try {
            MongoCollection<Document> collection = getAnimalsCollection();
            Document doc = animalToDocument(animal);
            
            if (animal.getId() == null) {
                // Insert new animal
                collection.insertOne(doc);
                animal.setId(doc.getObjectId("_id"));
            } else {
                // Update existing animal
                collection.replaceOne(
                    Filters.eq("_id", animal.getId()),
                    doc
                );
            }
            
            return animal;
        } catch (Exception e) {
            System.out.println("Error saving animal: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Animal updateAnimal(Animal animal) {
        try {
            System.out.println("Updating animal with ID: " + animal.getId());
            return save(animal);
        } catch (Exception e) {
            System.out.println("Error updating animal: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public boolean deleteById(String id) {
        try {
            MongoCollection<Document> collection = getAnimalsCollection();
            return collection.deleteOne(Filters.eq("_id", new ObjectId(id))).getDeletedCount() > 0;
        } catch (Exception e) {
            System.out.println("Error deleting animal: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteByFarmerAndId(String farmerId, String animalId) {
        try {
            MongoCollection<Document> collection = getAnimalsCollection();
            return collection.deleteOne(Filters.and(
                Filters.eq("_id", new ObjectId(animalId)),
                Filters.eq("farmer", new ObjectId(farmerId))
            )).getDeletedCount() > 0;
        } catch (Exception e) {
            System.out.println("Error deleting animal by farmer and ID: " + e.getMessage());
            return false;
        }
    }

    private Animal documentToAnimal(Document doc) {
        try {
            Animal animal = new Animal();
            animal.setId(doc.getObjectId("_id"));
            animal.setName(doc.getString("name"));
            animal.setType(doc.getString("type"));
            animal.setBreed(doc.getString("breed"));
            animal.setAge(doc.getInteger("age"));
            animal.setGender(doc.getString("gender"));
            animal.setDetails(doc.getString("details"));
            animal.setPhoto_url(doc.getString("photo_url"));
            animal.setFarmer(doc.getObjectId("farmer"));
            
            // Handle createdAt field safely
            Object createdAtObj = doc.get("createdAt");
            if (createdAtObj instanceof Date) {
                animal.setCreatedAt((Date) createdAtObj);
            } else if (createdAtObj instanceof String) {
                try {
                    animal.setCreatedAt(dateFormat.parse((String) createdAtObj));
                } catch (Exception e) {
                    animal.setCreatedAt(new Date());
                }
            } else {
                animal.setCreatedAt(new Date());
            }
            
            return animal;
        } catch (Exception e) {
            System.out.println("Error converting document to animal: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private Document animalToDocument(Animal animal) {
        Document doc = new Document();
        if (animal.getId() != null) {
            doc.append("_id", animal.getId());
        }
        doc.append("name", animal.getName())
           .append("type", animal.getType())
           .append("breed", animal.getBreed())
           .append("age", animal.getAge())
           .append("gender", animal.getGender())
           .append("details", animal.getDetails())
           .append("photo_url", animal.getPhoto_url())
           .append("farmer", animal.getFarmer())
           .append("createdAt", animal.getCreatedAt());
        return doc;
    }
}