package com.energyter.app.mongodb.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class DatabaseConnection {

    private static final String URI = "mongodb+srv://germanospina12014:admin123456789@ecosystemaccountcluster.3wsyrgk.mongodb.net/?retryWrites=true&w=majority&appName=EcosystemAccountClusterTesis";

    public static MongoCollection<Document> getCollection() {
        try (MongoClient mongoClient = MongoClients.create(URI)) {
            MongoDatabase database = mongoClient.getDatabase("ecosystem_accounts");
            MongoCollection<Document> collection = database.getCollection("performance");
            return collection;
        }
    }
}
