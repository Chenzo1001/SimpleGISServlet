# SimpleGISServlet
A simple GIS servlet with HDFS/PostgreSQL/MongoDB to display & process spatial data

## 1.本项目核心功能

项目集成了将栅格影像或矢量数据从切割，到数据库或HDFS存储，到OpenLayer发布展示的功能。

```Const```->项目所需运行常量

```TilesSplit```->影像分割成块以及重采样过程

```Image*```->数据源连接

```Output*```->数据的处理输出Servlet

```UploadServlet```->简要的数据上传Servlet

## 2.项目构建运行

```TilesSplit``` 模块可以通过直接运行test下的```Split.java```以执行自动化分割和存储。项目servlet以 ```tomcat``` 为基础提供服务

## Enjoy Yourself !
