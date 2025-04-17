package com.energyter.app.mongodb.services.impl;

import com.energyter.app.mongodb.database.DatabaseConnection;
import com.energyter.app.mongodb.services.InsertOneDocument;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class InsertOneDocumentImpl implements InsertOneDocument {
    @Override
    public void insertOne(Document document) {
        MongoCollection<Document> collection = DatabaseConnection.getCollection();
        collection.insertOne(document);
    }
}
