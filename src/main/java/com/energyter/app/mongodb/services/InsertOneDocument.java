package com.energyter.app.mongodb.services;

import com.energyter.app.mongodb.database.DatabaseConnection;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public interface InsertOneDocument {

    public void insertOne(Document document);

}
