package com.elliot.spatialimagequery;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.IOException;

@WebServlet(name = "OutputImage", value = "/raster")
public class OutputImage extends HttpServlet {

    public void init() {
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {

        int L = Integer.parseInt(request.getParameter("L"));
        int R = Integer.parseInt(request.getParameter("R"));
        int C = Integer.parseInt(request.getParameter("C"));
        ImageDB qImage = new ImageDB();
        // ImageHDFS qImage = new ImageHDFS();
        byte[] image = qImage.queryRaster(L,R,C);
        try {
            response.setContentType("image/jpeg");
            // 将byte[]写入响应流
            response.getOutputStream().write(image);
        }catch (IOException e)
        {e.printStackTrace();}
    }
}