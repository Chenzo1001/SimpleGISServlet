package com.elliot.spatialimagequery;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.commons.io.IOUtils;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static com.elliot.spatialimagequery.TilesTransform.*;

public class SplitIntoTiles{
    FileSystem fs;
    public SplitIntoTiles() throws IOException {
        String hdfsUri = "hdfs://server:8020";
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsUri);
        fs = FileSystem.get(conf);
    }
    public Dataset getTransformDs(Dataset ori_ds) throws Exception {
        if (ori_ds == null) {
            throw new Exception();
        }
        SpatialReference ori_SRS = new SpatialReference(ori_ds.GetProjection());
        SpatialReference wgs84 = new SpatialReference();
        wgs84.ImportFromEPSG(4326);
        CoordinateTransformation transform = new CoordinateTransformation(ori_SRS, wgs84);
        Dataset memDataset = gdal.GetDriverByName("MEM").Create("", ori_ds.GetRasterXSize(), ori_ds.GetRasterYSize(), ori_ds.GetRasterCount());

        // 设置投影信息
        //memDataset.SetProjection(wgs84.ExportToWkt());

        // 设置地理变换信息
        double[] geoTransform = new double[6];
        ori_ds.GetGeoTransform(geoTransform);
        transform.TransformPoint(geoTransform);
        memDataset.SetGeoTransform(geoTransform);

        // 进行坐标转换并写入数据
        int[] buffer = new int[ori_ds.GetRasterXSize() * ori_ds.GetRasterYSize()];
        for (int i = 1; i <= Math.max(3, ori_ds.getRasterCount()); ++i) {
            ori_ds.GetRasterBand(i).ReadRaster(0, 0, ori_ds.GetRasterXSize(), ori_ds.GetRasterYSize(), buffer);
            memDataset.GetRasterBand(i).WriteRaster(0, 0, ori_ds.GetRasterXSize(), ori_ds.GetRasterYSize(), buffer);
        }

        return memDataset;
    }

    public void generateTiles(Dataset dataset) throws FactoryException {
        double[] ori_transform = dataset.GetGeoTransform();
        int rasterCount = dataset.GetRasterCount();

        int yCount = dataset.getRasterYSize();
        int xCount = dataset.GetRasterXSize();
        double latMin = ori_transform[3] - (yCount * ori_transform[1]);
        double latMax = ori_transform[3] ;
        double lonMin = ori_transform[0];
        double lonMax = ori_transform[0] + (xCount * ori_transform[1]);

        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326", true);
        ReferencedEnvelope imageBound = new ReferencedEnvelope(lonMin, lonMax, latMin, latMax, crs);
        int zoom_min = zoomForPixelSize(ori_transform[1] * Math.max(yCount, xCount) / 256.0);
        int zoom_max = zoomForPixelSize(ori_transform[1]);
        double src_w_e_pixel_resolution = (lonMax - lonMin) / xCount;
        // 原始图像南北方向像素分辨率
        double src_n_s_pixel_resolution = (latMax - latMin) / yCount;
        for (int zoom = zoom_min; zoom <= zoom_max; ++zoom){
            int tileRowMax = lat2tile(latMin, zoom);
            int tileRowMin = lat2tile(latMax, zoom);
            int tileColMin = lon2tile(lonMin, zoom);
            int tileColMax = lon2tile(lonMax, zoom);
            String output = tileRowMax + " " + tileRowMin + " " + tileColMax + " " + tileColMin;
            System.out.println("Zoom:" + zoom);
            System.out.println(output);
            for (int row = tileRowMin; row <= tileRowMax; ++row)
                for (int col = tileColMin; col <= tileColMax; ++col) {
                    double tmpLatMin = tile2lat(row + 1, zoom);
                    double tmpLatMax = tile2lat(row, zoom);

                    double tmpLonMin = tile2lon(col, zoom);
                    double tmpLonMax = tile2lon(col + 1, zoom);
                    System.out.println("x1=" + tmpLonMin + "y1=" + tmpLatMax + "x2=" + tmpLonMax + "y2=" + tmpLatMin);
                    ReferencedEnvelope tileBound = new ReferencedEnvelope(tmpLonMin, tmpLonMax, tmpLatMin, tmpLatMax, crs);
                    ReferencedEnvelope intersect = tileBound.intersection(imageBound);

                    double dst_w_e_pixel_resolution = (tmpLonMax - tmpLonMin) / 256;
                    double dst_n_s_pixel_resolution = (tmpLatMax - tmpLatMin) / 256;

                    int offset_x = (int) ((intersect.getMinX() - lonMin) / src_w_e_pixel_resolution);
                    int offset_y = (int) Math.abs((intersect.getMaxY() - latMax) /src_n_s_pixel_resolution);

                    int block_xsize = (int) ((intersect.getMaxX() - intersect.getMinX()) / src_w_e_pixel_resolution);
                    int block_ysize = (int) ((intersect.getMaxY() - intersect.getMinY()) / src_n_s_pixel_resolution);
                    int image_Xbuf = (int) Math.ceil((intersect.getMaxX() - intersect.getMinX()) / dst_w_e_pixel_resolution);
                    int image_Ybuf = (int) Math.ceil(Math.abs((intersect.getMaxY() - intersect.getMinY()) / dst_n_s_pixel_resolution));

                    // 求原始图像在切片中的偏移坐标
                    int imageOffsetX = (int) ((intersect.getMinX() - tmpLonMin) / dst_w_e_pixel_resolution);
                    int imageOffsetY = (int) Math.abs((intersect.getMaxY() - tmpLatMax) / dst_n_s_pixel_resolution);
                    imageOffsetX = Math.max(imageOffsetX, 0);
                    imageOffsetY = Math.max(imageOffsetY, 0);

                    Band in_band1 = dataset.GetRasterBand(1);
                    Band in_band2 = dataset.GetRasterBand(2);
                    Band in_band3 = dataset.GetRasterBand(3);
                    int[] band1BuffData = new int[256 * 256 * gdalconst.GDT_Int32];
                    int[] band2BuffData = new int[256 * 256 * gdalconst.GDT_Int32];
                    int[] band3BuffData = new int[256 * 256 * gdalconst.GDT_Int32];

                    in_band1.ReadRaster(offset_x, offset_y, block_xsize, block_ysize, image_Xbuf, image_Ybuf, gdalconst.GDT_Int32, band1BuffData, 0, 0);
                    in_band2.ReadRaster(offset_x, offset_y, block_xsize, block_ysize, image_Xbuf, image_Ybuf, gdalconst.GDT_Int32, band2BuffData, 0, 0);
                    in_band3.ReadRaster(offset_x, offset_y, block_xsize, block_ysize, image_Xbuf, image_Ybuf, gdalconst.GDT_Int32, band3BuffData, 0, 0);

                    Driver memDriver = gdal.GetDriverByName("MEM");
                    Dataset msmDS = memDriver.Create("msmDS", 256, 256, 3);
                    Band dstBand1 = msmDS.GetRasterBand(1);
                    Band dstBand2 = msmDS.GetRasterBand(2);
                    Band dstBand3 = msmDS.GetRasterBand(3);

                    //Band alphaBand = msmDS.GetRasterBand(4);
                    int[] alphaData = new int[256 * 256 * gdalconst.GDT_Int32];
                    for (int index = 0; index < alphaData.length; index++) {
                        if (band1BuffData[index] > 0) {
                            alphaData[index] = 255;
                        }
                    }
                    dstBand1.WriteRaster(imageOffsetX, imageOffsetY, image_Xbuf, image_Ybuf, band1BuffData);
                    dstBand2.WriteRaster(imageOffsetX, imageOffsetY, image_Xbuf, image_Ybuf, band2BuffData);
                    dstBand3.WriteRaster(imageOffsetX, imageOffsetY, image_Xbuf, image_Ybuf, band3BuffData);

                    String pngPath = "img1" + File.separator + zoom + "c" + col + "r" + row + ".jpg";
                    Driver pngDriver = gdal.GetDriverByName("JPEG");

                    Dataset pngDs = pngDriver.CreateCopy(pngPath, msmDS);

                    msmDS.FlushCache();
                    pngDs.delete();

                    saveToHDFS(pngPath, "/image",row, col, zoom);
                }

        }
    }

    public void saveToDataBase(String filePath, int row, int col, int zoom) {
        try {
            File tileFile = new File(filePath);
            InputStream fis = new FileInputStream(tileFile);


            ImageDB imgDB=new ImageDB();

            Connection con = imgDB.getConnection();

            if (con != null) {
                String sql = "INSERT INTO \"RSImageQuery\" (\"LEVEL\",\"ROW\",\"COL\",\"IMG\") VALUES(?,?,?,?)";
                PreparedStatement preparedStatement = con.prepareStatement(sql);
                preparedStatement.setInt(1, zoom);
                preparedStatement.setInt(2, row);
                preparedStatement.setInt(3, col);

                preparedStatement.setBinaryStream(4, fis, (int)tileFile.length());
                preparedStatement.execute();
            }
            tileFile.delete();
        }
         catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void saveToHDFS(String filePath, String outputPath, int row, int col, int zoom) {
        try {
            File tileFile = new File(filePath);
            InputStream fis = new FileInputStream(tileFile);

            String outputFilePath = outputPath + "/IMAGE_"+zoom + "/window_" + row + "_" + col + ".jpg";
            byte[] imageData = IOUtils.toByteArray(fis);;
            FSDataOutputStream out = fs.create(new Path(outputFilePath));
            out.write(imageData);

            tileFile.delete();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}