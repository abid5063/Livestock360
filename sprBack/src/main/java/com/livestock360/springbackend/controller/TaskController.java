package com.livestock360.springbackend.controller;

import com.livestock360.springbackend.model.Task;
import com.livestock360.springbackend.service.TaskService;
import com.livestock360.springbackend.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {
    
    @Autowired
    private TaskService taskService;
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * POST /api/tasks - Create a new task
     * Matches exactly the SimpleBackend.TaskHandler.handleCreateTask()
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTask(@RequestBody Map<String, Object> request,
                                                          @RequestHeader("Authorization") String authHeader) {
        try {
            // Validate JWT token
            String farmerId = extractFarmerIdFromToken(authHeader);
            if (farmerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Access denied. Invalid or expired token."));
            }
            
            // Extract request data
            String title = (String) request.get("title");
            String description = (String) request.get("description");
            String dueDateStr = (String) request.get("dueDate");
            String dueTime = (String) request.get("dueTime");
            Object estimatedCostObj = request.get("estimatedCost");
            String priority = (String) request.get("priority");
            String category = (String) request.get("category");
            String animalId = (String) request.get("animal");
            String notes = (String) request.get("notes");
            
            // Validation (exactly like SimpleBackend)
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Title is required"));
            }
            
            // Parse estimated cost
            Double estimatedCost = null;
            if (estimatedCostObj != null) {
                try {
                    if (estimatedCostObj instanceof String) {
                        estimatedCost = Double.parseDouble((String) estimatedCostObj);
                    } else if (estimatedCostObj instanceof Number) {
                        estimatedCost = ((Number) estimatedCostObj).doubleValue();
                    }
                } catch (NumberFormatException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid estimated cost format"));
                }
            }
            
            // Parse due date
            Date dueDate = null;
            if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
                try {
                    dueDate = parseDate(dueDateStr);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid date format. Use YYYY-MM-DD"));
                }
            }
            
            // Create task object
            Task task = new Task();
            task.setTitle(title.trim());
            task.setDescription(description != null ? description.trim() : "");
            task.setDueDate(dueDate);
            task.setDueTime(dueTime);
            task.setEstimatedCost(estimatedCost);
            task.setPriority(priority != null ? priority : "medium");
            task.setStatus("pending");
            task.setCategory(category != null ? category : "other");
            task.setFarmer(new ObjectId(farmerId));
            task.setIsCompleted(false);
            task.setNotes(notes != null ? notes : "");
            task.setCreatedAt(new Date());
            task.setUpdatedAt(new Date());
            
            // Set animal if provided
            if (animalId != null && !animalId.trim().isEmpty()) {
                try {
                    task.setAnimal(new ObjectId(animalId));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid animal ID format"));
                }
            }
            
            // Save task
            Task savedTask = taskService.save(task);
            if (savedTask == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create task"));
            }
            
            // Return response with animal details (if present)
            Map<String, Object> response = taskService.createTaskResponse(savedTask);
            
            System.out.println("✅ Task created: " + title);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Internal server error"));
        }
    }

    /**
     * GET /api/tasks - Get all tasks with optional filters
     * Matches exactly the SimpleBackend.TaskHandler.handleGetTasks()
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getTasks(@RequestHeader("Authorization") String authHeader,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(required = false) String priority,
                                                             @RequestParam(required = false) String category,
                                                             @RequestParam(required = false) String animal,
                                                             @RequestParam(required = false) String dateFrom,
                                                             @RequestParam(required = false) String dateTo) {
        try {
            // Validate JWT token
            String farmerId = extractFarmerIdFromToken(authHeader);
            if (farmerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
            
            // Parse date filters
            Date dateFromParsed = null;
            Date dateToParsed = null;
            
            if (dateFrom != null && !dateFrom.trim().isEmpty()) {
                try {
                    dateFromParsed = parseDate(dateFrom);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
                }
            }
            
            if (dateTo != null && !dateTo.trim().isEmpty()) {
                try {
                    dateToParsed = parseDate(dateTo);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
                }
            }
            
            // Get tasks with filters
            List<Task> tasks = taskService.findByFarmerIdWithFilters(
                farmerId, status, priority, category, animal, dateFromParsed, dateToParsed);
            
            // Convert to response format with animal details
            List<Map<String, Object>> response = new ArrayList<>();
            for (Task task : tasks) {
                response.add(taskService.createTaskResponse(task));
            }
            
            System.out.println("✅ Tasks retrieved: " + tasks.size() + " tasks for farmer " + farmerId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * GET /api/tasks/{id} - Get a specific task by ID
     * Matches exactly the SimpleBackend task get by ID functionality
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTaskById(@PathVariable String id,
                                                          @RequestHeader("Authorization") String authHeader) {
        try {
            // Validate JWT token
            String farmerId = extractFarmerIdFromToken(authHeader);
            if (farmerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Access denied. Invalid or expired token."));
            }
            
            // Validate task ID format
            if (id.length() != 24 || !id.matches("[a-f0-9]{24}")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid task ID format"));
            }
            
            Task task = taskService.findById(id, farmerId);
            if (task == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Task not found"));
            }
            
            Map<String, Object> response = taskService.createTaskResponse(task);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Internal server error"));
        }
    }

    /**
     * PUT /api/tasks/{id} - Update a task
     * Matches exactly the SimpleBackend task update functionality
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateTask(@PathVariable String id,
                                                         @RequestBody Map<String, Object> updates,
                                                         @RequestHeader("Authorization") String authHeader) {
        try {
            // Validate JWT token
            String farmerId = extractFarmerIdFromToken(authHeader);
            if (farmerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Access denied. Invalid or expired token."));
            }
            
            // Validate task ID format
            if (id.length() != 24 || !id.matches("[a-f0-9]{24}")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid task ID format"));
            }
            
            Task task = taskService.findById(id, farmerId);
            if (task == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Task not found"));
            }
            
            // Apply updates
            if (updates.get("title") != null) {
                task.setTitle((String) updates.get("title"));
            }
            if (updates.get("description") != null) {
                task.setDescription((String) updates.get("description"));
            }
            if (updates.get("dueDate") != null) {
                try {
                    task.setDueDate(parseDate((String) updates.get("dueDate")));
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid date format"));
                }
            }
            if (updates.get("dueTime") != null) {
                task.setDueTime((String) updates.get("dueTime"));
            }
            if (updates.get("estimatedCost") != null) {
                Object costObj = updates.get("estimatedCost");
                try {
                    if (costObj instanceof String) {
                        task.setEstimatedCost(Double.parseDouble((String) costObj));
                    } else if (costObj instanceof Number) {
                        task.setEstimatedCost(((Number) costObj).doubleValue());
                    }
                } catch (NumberFormatException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid estimated cost format"));
                }
            }
            if (updates.get("priority") != null) {
                task.setPriority((String) updates.get("priority"));
            }
            if (updates.get("status") != null) {
                task.setStatus((String) updates.get("status"));
            }
            if (updates.get("category") != null) {
                task.setCategory((String) updates.get("category"));
            }
            if (updates.get("notes") != null) {
                task.setNotes((String) updates.get("notes"));
            }
            if (updates.get("animal") != null) {
                String animalId = (String) updates.get("animal");
                if (animalId != null && !animalId.trim().isEmpty()) {
                    try {
                        task.setAnimal(new ObjectId(animalId));
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Invalid animal ID format"));
                    }
                } else {
                    task.setAnimal(null);
                }
            }
            
            Task updatedTask = taskService.updateTask(task);
            if (updatedTask == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update task"));
            }
            
            Map<String, Object> response = taskService.createTaskResponse(updatedTask);
            System.out.println("✅ Task updated: " + updatedTask.getTitle());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Internal server error"));
        }
    }

    /**
     * PATCH /api/tasks/{id}/toggle - Toggle task completion status
     * Matches exactly the SimpleBackend task toggle functionality
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleTaskCompletion(@PathVariable String id,
                                                                   @RequestHeader("Authorization") String authHeader) {
        try {
            // Validate JWT token
            String farmerId = extractFarmerIdFromToken(authHeader);
            if (farmerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Access denied. Invalid or expired token."));
            }
            
            // Validate task ID format
            if (id.length() != 24 || !id.matches("[a-f0-9]{24}")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid task ID format"));
            }
            
            Task updatedTask = taskService.toggleTaskCompletion(id, farmerId);
            if (updatedTask == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Task not found"));
            }
            
            Map<String, Object> response = taskService.createTaskResponse(updatedTask);
            System.out.println("✅ Task completion toggled: " + updatedTask.getTitle() + 
                             " (completed: " + updatedTask.getIsCompleted() + ")");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Internal server error"));
        }
    }

    /**
     * DELETE /api/tasks/{id} - Delete a task
     * Matches exactly the SimpleBackend task delete functionality
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteTask(@PathVariable String id,
                                                         @RequestHeader("Authorization") String authHeader) {
        try {
            // Validate JWT token
            String farmerId = extractFarmerIdFromToken(authHeader);
            if (farmerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Access denied. Invalid or expired token."));
            }
            
            // Validate task ID format
            if (id.length() != 24 || !id.matches("[a-f0-9]{24}")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid task ID format"));
            }
            
            boolean deleted = taskService.deleteById(id, farmerId);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Task not found"));
            }
            
            System.out.println("✅ Task deleted: " + id);
            return ResponseEntity.ok(Map.of("message", "Task deleted successfully"));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Internal server error"));
        }
    }

    /**
     * GET /api/tasks/stats/overview - Get task statistics
     * Matches exactly the SimpleBackend.TaskHandler.handleGetTaskStats()
     */
    @GetMapping("/stats/overview")
    public ResponseEntity<Map<String, Object>> getTaskStats(@RequestHeader("Authorization") String authHeader) {
        try {
            // Validate JWT token
            String farmerId = extractFarmerIdFromToken(authHeader);
            if (farmerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
            
            Map<String, Object> stats = taskService.getTaskStats(farmerId);
            
            System.out.println("✅ Task stats retrieved for farmer: " + farmerId);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Helper methods
    private String extractFarmerIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        
        String token = authHeader.substring(7);
        Claims claims = JwtUtil.validateTokenStatic(token);
        if (claims == null) {
            return null;
        }
        
        return JwtUtil.getUserIdFromToken(token);
    }

    private Date parseDate(String dateString) throws Exception {
        try {
            // Try ISO format first (YYYY-MM-DD)
            if (dateString.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return dateFormat.parse(dateString);
            }
            // Try timestamp format
            return new Date(Long.parseLong(dateString));
        } catch (Exception e) {
            throw new Exception("Invalid date format");
        }
    }
}