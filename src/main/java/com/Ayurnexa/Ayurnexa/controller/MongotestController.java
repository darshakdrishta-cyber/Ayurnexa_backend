package com.Ayurnexa.Ayurnexa.controller;   
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;



@RestController 
@RequestMapping ("/api/mongo")
public class MongotestController {
    private final MongoTemplate mongoTemplate;

    public MongotestController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/test-mongo")
    public String testMongoConnection() {
        try {
            Document dbName = mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1));
            System.out.println("Successfully connected to MongoDB database: " + dbName);
            if(dbName == null || dbName.isEmpty()) {
                return "Failed to connect to MongoDB: Database name is null or empty";
            }
            return "Successfully connected to MongoDB database: " + dbName;
        } catch (Exception e) {
             System.out.println("Not success");
            return "Failed to connect to MongoDB: " + e.getMessage();
        }
    }
}
