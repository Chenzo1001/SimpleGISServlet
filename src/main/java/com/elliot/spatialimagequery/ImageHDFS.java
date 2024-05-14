package com.elliot.spatialimagequery;

import jakarta.servlet.ServletException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

public class ImageHDFS {
    private FileSystem fs;
    public ImageHDFS() {
        try {
            Configuration conf = new Configuration();
            String hdfsUri = "hdfs://server:8020";
            conf.set("fs.defaultFS", hdfsUri);
            fs = FileSystem.get(conf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] queryRaster(int level, int row, int col) throws ServletException {
        byte[] imageData = null;
        String hdfsFilePath = "/image/" + level + "c" + col + "r" + row + ".jpg";

        try {
            Path hdfsPath = new Path(hdfsFilePath);
            if (fs.exists(hdfsPath)) {
                try (FSDataInputStream inputStream = fs.open(hdfsPath);
                     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    imageData = outputStream.toByteArray();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new ServletException("Image "+hdfsFilePath+" Not Found!!");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new ServletException("Error reading image from HDFS!!");
        }

        return imageData;
    }
}
