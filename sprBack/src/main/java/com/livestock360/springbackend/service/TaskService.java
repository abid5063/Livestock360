package com.livestock360.springbackend.service;

import com.livestock360.springbackend.model.Task;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskService {

    private final MongoDatabase database;

    @Autowired
    public TaskService(MongoDatabase database) {
        this.database = database;
    }

    private MongoCollection<Document> getTasksCollection() {
        return database.getCollection("tasks");
    }

    private MongoCollection<Document> getAnimalsCollection() {
        return database.getCollection("animals");
    }

    public Task save(Task task) {
        try {
            MongoCollection<Document> collection = getTasksCollection();
            Document doc = taskToDocument(task);
            
            if (task.getId() == null) {
                // Insert new task
                collection.insertOne(doc);
                task.setId(doc.getObjectId("_id"));
            } else {
                // Update existing task
                collection.replaceOne(
                    Filters.eq("_id", task.getId()),
                    doc
                );
            }
            
            return task;
        } catch (Exception e) {
            System.out.println("Error saving task: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Task findById(String id, String farmerId) {
        try {
            MongoCollection<Document> collection = getTasksCollection();
            Document doc = collection.find(
                Filters.and(
                    Filters.eq("_id", new ObjectId(id)),
                    Filters.eq("farmer", new ObjectId(farmerId))
                )
            ).first();
            
            if (doc != null) {
                return documentToTask(doc);
            }
            return null;
        } catch (Exception e) {
            System.out.println("Error finding task by ID: " + e.getMessage());
            return null;
        }
    }

    public List<Task> findByFarmerId(String farmerId) {
        try {
            MongoCollection<Document> collection = getTasksCollection();
            List<Task> tasks = new ArrayList<>();
            
            collection.find(Filters.eq("farmer", new ObjectId(farmerId)))
                .sort(new Document("dueDate", 1).append("dueTime", 1))
                .forEach(doc -> {
                    Task task = documentToTask(doc);
                    if (task != null) {
                        tasks.add(task);
                    }
                });
            
            return tasks;
        } catch (Exception e) {
            System.out.println("Error finding tasks by farmer ID: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Task> findByFarmerIdWithFilters(String farmerId, String status, String priority, 
                                               String category, String animalId, Date dateFrom, Date dateTo) {
        try {
            MongoCollection<Document> collection = getTasksCollection();
            List<Task> tasks = new ArrayList<>();
            
            // Build query filters
            List<org.bson.conversions.Bson> filters = new ArrayList<>();
            filters.add(Filters.eq("farmer", new ObjectId(farmerId)));
            
            if (status != null && !status.trim().isEmpty()) {
                filters.add(Filters.eq("status", status));
            }
            if (priority != null && !priority.trim().isEmpty()) {
                filters.add(Filters.eq("priority", priority));
            }
            if (category != null && !category.trim().isEmpty()) {
                filters.add(Filters.eq("category", category));
            }
            if (animalId != null && !animalId.trim().isEmpty()) {
                filters.add(Filters.eq("animal", new ObjectId(animalId)));
            }
            
            // Date range filter
            if (dateFrom != null || dateTo != null) {
                List<org.bson.conversions.Bson> dateFilters = new ArrayList<>();
                if (dateFrom != null) {
                    dateFilters.add(Filters.gte("dueDate", dateFrom));
                }
                if (dateTo != null) {
                    dateFilters.add(Filters.lte("dueDate", dateTo));
                }
                if (!dateFilters.isEmpty()) {
                    filters.add(Filters.and(dateFilters));
                }
            }
            
            org.bson.conversions.Bson combinedFilter = Filters.and(filters);
            
            collection.find(combinedFilter)
                .sort(new Document("dueDate", 1).append("dueTime", 1))
                .forEach(doc -> {
                    Task task = documentToTask(doc);
                    if (task != null) {
                        tasks.add(task);
                    }
                });
            
            return tasks;
        } catch (Exception e) {
            System.out.println("Error finding tasks with filters: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean deleteById(String id, String farmerId) {
        try {
            MongoCollection<Document> collection = getTasksCollection();
            return collection.deleteOne(
                Filters.and(
                    Filters.eq("_id", new ObjectId(id)),
                    Filters.eq("farmer", new ObjectId(farmerId))
                )
            ).getDeletedCount() > 0;
        } catch (Exception e) {
            System.out.println("Error deleting task: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteByAnimalAndFarmer(String animalId, String farmerId) {
        try {
            MongoCollection<Document> collection = getTasksCollection();
            return collection.deleteMany(
                Filters.and(
                    Filters.eq("animal", new ObjectId(animalId)),
                    Filters.eq("farmer", new ObjectId(farmerId))
                )
            ).getDeletedCount() >= 0;
        } catch (Exception e) {
            System.out.println("Error deleting tasks for animal: " + e.getMessage());
            return false;
        }
    }

    public Task updateTask(Task task) {
        try {
            task.setUpdatedAt(new Date());
            return save(task);
        } catch (Exception e) {
            System.out.println("Error updating task: " + e.getMessage());
            return null;
        }
    }

    public Task toggleTaskCompletion(String id, String farmerId) {
        try {
            Task task = findById(id, farmerId);
            if (task == null) {
                return null;
            }
            
            boolean newCompletionStatus = !task.getIsCompleted();
            task.setIsCompleted(newCompletionStatus);
            task.setStatus(newCompletionStatus ? "completed" : "pending");
            task.setCompletedAt(newCompletionStatus ? new Date() : null);
            task.setUpdatedAt(new Date());
            
            return save(task);
        } catch (Exception e) {
            System.out.println("Error toggling task completion: " + e.getMessage());
            return null;
        }
    }

    public Map<String, Object> getTaskStats(String farmerId) {
        try {
            MongoCollection<Document> collection = getTasksCollection();
            List<Document> allTasks = collection.find(Filters.eq("farmer", new ObjectId(farmerId)))
                .into(new ArrayList<>());
            
            // Calculate statistics
            int totalTasks = allTasks.size();
            int completedTasks = 0;
            int pendingTasks = 0;
            double totalEstimatedCost = 0.0;
            int overdueTasks = 0;
            
            Date now = new Date();
            
            for (Document task : allTasks) {
                Boolean isCompleted = task.getBoolean("isCompleted", false);
                String status = task.getString("status");
                Date dueDate = task.getDate("dueDate");
                Double cost = task.getDouble("estimatedCost");
                
                if (isCompleted != null && isCompleted) completedTasks++;
                if ("pending".equals(status)) pendingTasks++;
                if (cost != null) totalEstimatedCost += cost;
                
                // Check if overdue (due date passed and not completed)
                if (dueDate != null && dueDate.before(now) && (isCompleted == null || !isCompleted)) {
                    overdueTasks++;
                }
            }
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTasks", totalTasks);
            stats.put("completedTasks", completedTasks);
            stats.put("pendingTasks", pendingTasks);
            stats.put("totalEstimatedCost", totalEstimatedCost);
            stats.put("overdueTasks", overdueTasks);
            
            return stats;
        } catch (Exception e) {
            System.out.println("Error calculating task stats: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public Map<String, Object> createTaskResponse(Task task) {
        Map<String, Object> response = new HashMap<>();
        response.put("_id", task.getId().toString());
        response.put("title", task.getTitle());
        response.put("description", task.getDescription());
        response.put("dueDate", task.getDueDate());
        response.put("dueTime", task.getDueTime());
        response.put("estimatedCost", task.getEstimatedCost());
        response.put("priority", task.getPriority());
        response.put("status", task.getStatus());
        response.put("category", task.getCategory());
        response.put("isCompleted", task.getIsCompleted());
        response.put("notes", task.getNotes());
        response.put("createdAt", task.getCreatedAt());
        response.put("updatedAt", task.getUpdatedAt());
        response.put("completedAt", task.getCompletedAt());
        
        // Add animal details if present
        if (task.getAnimal() != null) {
            MongoCollection<Document> animals = getAnimalsCollection();
            Document animal = animals.find(Filters.eq("_id", task.getAnimal())).first();
            if (animal != null) {
                Map<String, Object> animalData = new HashMap<>();
                animalData.put("_id", animal.getObjectId("_id").toString());
                animalData.put("name", animal.getString("name"));
                animalData.put("type", animal.getString("type"));
                animalData.put("breed", animal.getString("breed"));
                response.put("animal", animalData);
            }
        }
        
        return response;
    }

    private Task documentToTask(Document doc) {
        try {
            Task task = new Task();
            task.setId(doc.getObjectId("_id"));
            task.setTitle(doc.getString("title"));
            task.setDescription(doc.getString("description"));
            task.setDueDate(doc.getDate("dueDate"));
            task.setDueTime(doc.getString("dueTime"));
            task.setEstimatedCost(doc.getDouble("estimatedCost"));
            task.setPriority(doc.getString("priority"));
            task.setStatus(doc.getString("status"));
            task.setCategory(doc.getString("category"));
            task.setFarmer(doc.getObjectId("farmer"));
            task.setAnimal(doc.getObjectId("animal"));
            task.setIsCompleted(doc.getBoolean("isCompleted"));
            task.setNotes(doc.getString("notes"));
            task.setCreatedAt(doc.getDate("createdAt"));
            task.setUpdatedAt(doc.getDate("updatedAt"));
            task.setCompletedAt(doc.getDate("completedAt"));
            return task;
        } catch (Exception e) {
            System.out.println("Error converting document to task: " + e.getMessage());
            return null;
        }
    }

    private Document taskToDocument(Task task) {
        Document doc = new Document();
        if (task.getId() != null) {
            doc.append("_id", task.getId());
        }
        doc.append("title", task.getTitle())
           .append("description", task.getDescription())
           .append("dueDate", task.getDueDate())
           .append("dueTime", task.getDueTime())
           .append("estimatedCost", task.getEstimatedCost())
           .append("priority", task.getPriority())
           .append("status", task.getStatus())
           .append("category", task.getCategory())
           .append("farmer", task.getFarmer())
           .append("animal", task.getAnimal())
           .append("isCompleted", task.getIsCompleted())
           .append("notes", task.getNotes())
           .append("createdAt", task.getCreatedAt())
           .append("updatedAt", task.getUpdatedAt())
           .append("completedAt", task.getCompletedAt());
        return doc;
    }
}