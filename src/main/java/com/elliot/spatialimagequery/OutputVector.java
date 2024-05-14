package com.elliot.spatialimagequery;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import com.mongodb.client.*;
import org.bson.Document;
import java.io.IOException;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@WebServlet(name = "VectorServlet", value = "/vector")
public class OutputVector extends HttpServlet {
    private static final String DATABASE_NAME = "test";
    private static final String COLLECTION_NAME = "China";

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

            double x1 = Double.parseDouble(request.getParameter("x1"));
            double x2 = Double.parseDouble(request.getParameter("x2"));
            double y1 = Double.parseDouble(request.getParameter("y1"));
            double y2 = Double.parseDouble(request.getParameter("y2"));
            String geojson = getGeojson(x1, x2, y1, y2);
        try(var out = response.getWriter()) {
            response.setContentType("application/json");
            out.println(geojson);
        }catch(IOException e) {
        }

    }

    public String getGeojson(double x1, double x2, double y1, double y2) {
        try {
            MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
            MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

            // 构造查询条件
            Document query = new Document("$or", Arrays.asList(
                    new Document("features.geometry.coordinates",
                            new Document("$elemMatch",
                                    new Document("$elemMatch",
                                            new Document("$gte", x1).append("$lte", x2)))
                                    .append("$elemMatch",
                                            new Document("$gte", y1).append("$lte", y2))),
                    new Document("features.geometry.coordinates",
                            new Document("$elemMatch",
                                    new Document("$elemMatch",
                                            new Document("$elemMatch",
                                                    new Document("$gte", x1).append("$lte", x2))
                                    ).append("$elemMatch",
                                            new Document("$gte", y1).append("$lte", y2)))),
                    new Document("features.geometry.coordinates",
                            new Document("$elemMatch",
                                    new Document("$elemMatch",
                                            new Document("$elemMatch",
                                                    new Document("$elemMatch",
                                                            new Document("$gte", x1).append("$lte", x2))
                                            )
                                    ).append("$elemMatch",
                                            new Document("$elemMatch",
                                                    new Document("$gte", y1).append("$lte", y2))
                                    )
                            )
                    )
            ));

            // 执行查询
            FindIterable<Document> cursor = collection.find(query);
            // 遍历查询结果
            String geojson = "";
            for (Document doc : cursor) {
                geojson = (doc.toJson());
            }
            // 关闭 MongoDB 连接
            mongoClient.close();
            return geojson;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}