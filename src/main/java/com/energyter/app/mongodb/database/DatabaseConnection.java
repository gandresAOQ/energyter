package com.energyter.app.mongodb.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class DatabaseConnection {

    private static final String URI = "mongodb+srv://germanospina12014:<db_password>@ecosystemaccountcluster.3wsyrgk.mongodb.net/?retryWrites=true&w=majority&appName=EcosystemAccountClusterTesis";

    public static MongoCollection<Document> getCollection() {
        try (MongoClient mongoClient = MongoClients.create(URI)) {
            MongoDatabase database = mongoClient.getDatabase("sample_mflix");
            MongoCollection<Document> collection = database.getCollection("movies");
            return collection;
        }
    }
}
