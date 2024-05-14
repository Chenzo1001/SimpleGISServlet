package com.elliot.spatialimagequery;

import jakarta.servlet.ServletException;
import java.io.*;
import java.sql.*;

public class ImageDB {
    Connection conn=null;
    private String host, port, db, user, passwd;
    public ImageDB() {
        this.host = Const.host;
        this.db = Const.db;
        this.user = Const.user;
        this.passwd = Const.passwd;
        this.port = Const.port;
    }

    private void getConnectDB() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        try{
            conn = DriverManager.getConnection("jdbc:postgresql://"+host+":"+port+"/"+db, user, passwd);
        }catch(SQLException e)
        {
            e.printStackTrace();
        }
    }

    public Connection getConnection(){
        return conn;
    }
    public byte[] queryRaster(int level, int row, int col)  throws ServletException {
        //链接数据库
        getConnectDB();
        // 从数据库中查询影像数据
        byte[] imageData = null;

        String sQuery = "SELECT \"IMG\" FROM \"RSImageQuery\" WHERE \"LEVEL\"=? AND \"ROW\"=? AND \"COL\"=?";

        try (
             PreparedStatement stmt = conn.prepareStatement(sQuery);
        ) {
            stmt.setInt(1, level);
            stmt.setInt(2, row);
            stmt.setInt(3, col);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    imageData = rs.getBytes("IMG");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (imageData != null) {
            return imageData;
        } else {
            throw new ServletException("Image Not Found!!");
        }
    }


    public void write(String fileName,InputStream fis) {
        try {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO filetable (file_name, file_data) VALUES (?, ?)");
            stmt.setString(1, fileName);
            stmt.setBinaryStream(2, fis);
            stmt.executeUpdate();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
    }

    public void destroy() {
            try {
                if (conn != null) conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

}
