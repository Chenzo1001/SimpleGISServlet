package com.elliot.spatialimagequery;

import java.io.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

@WebServlet(name = "UploadServlet", value = "/upload")
public class UploadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String fileName = "";
        InputStream fileContent = null;
        boolean uploadSuccess = false;

        ImageDB imageDB = new ImageDB();

        try {
            for (Part part : request.getParts()) {
                fileName = extractFileName(part);
                fileContent = part.getInputStream();
            }

            imageDB.write(fileName, fileContent);
            if (fileContent != null) fileContent.close();
            uploadSuccess = true;
            request.setAttribute("uploadSuccess", uploadSuccess);
            // 转发回上传页面
            request.getRequestDispatcher("MapLayer.html").forward(request, response);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    private String extractFileName(Part part) {
            String contentDisp = part.getHeader("content-disposition");
            String[] items = contentDisp.split(";");
            for (String s : items) {
                if (s.trim().startsWith("filename")) {
                    return s.substring(s.indexOf("=") + 2, s.length() - 1);
                }
            }
            return "";
        }

}
