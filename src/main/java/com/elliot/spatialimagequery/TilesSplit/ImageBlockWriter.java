package com.elliot.spatialimagequery;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;

public class ImageBlockWriter {

    public static void main(String[] args) {
        String hdfsUri = Const.hdfsUri;
        // 要写入的文件路径
        String inputFilePath = "./Image.tif";

        // HDFS 文件系统配置
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsUri);

        try {
            // 连接到 HDFS 文件系统
            FileSystem fs = FileSystem.get(conf);

            // 读取原始影像文件
            BufferedImage originalImage = readTiffImage(inputFilePath);
            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            // 定义分块大小
            int blockSize = 256;
            int level = 0;

            // 对影像进行多级分块和重采样，直到影像大小小于 256x256
            while (width >= 256 && height >= 256) {
                // 创建当前级别的文件夹
                String outputDirectory = "/image" + "/level_" + level + "/";
                fs.mkdirs(new Path(outputDirectory));

                // 分块并重采样
                int xBlocks = (int) Math.ceil((double) width / blockSize);
                int yBlocks = (int) Math.ceil((double) height / blockSize);

                // 将影像分块保存到 HDFS
                for (int y = 0; y < yBlocks; y++) {
                    for (int x = 0; x < xBlocks; x++) {
                        // 计算当前分块的起始坐标
                        int startX = x * blockSize;
                        int startY = y * blockSize;
                        // 计算当前分块的宽度和高度
                        int blockWidth = Math.min(blockSize, width - startX);
                        int blockHeight = Math.min(blockSize, height - startY);
                        // 获取当前分块的子图
                        BufferedImage subImage = originalImage.getSubimage(startX, startY, blockWidth, blockHeight);
                        // 构建分块文件路径
                        String outputFilePath = outputDirectory + "window_" + x + "_" + y + ".jpg";
                        // 在 HDFS 上创建文件并写入子图
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(subImage, "tif", baos);
                        byte[] imageData = baos.toByteArray();
                        FSDataOutputStream out = fs.create(new Path(outputFilePath));
                        out.write(imageData);
                        out.close();
                        baos.close();
                    }
                }

                // 更新影像大小并增加级别
                width /= 2;
                height /= 2;
                level++;
            }

            // 关闭文件系统连接
            fs.close();

            System.out.println("影像成功分块并写入 HDFS！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static BufferedImage readTiffImage(String filePath) throws IOException {
        File file = new File(filePath);
        ImageInputStream iis = ImageIO.createImageInputStream(file);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
        if (!readers.hasNext()) {
            throw new IOException("No TIFF image reader found");
        }
        ImageReader reader = readers.next();
        reader.setInput(iis);
        return reader.read(0);
    }
}