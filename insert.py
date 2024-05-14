import os
import re
import psycopg2

tablename = "RSImageQuery"

# 连接到 PostgreSQL 数据库
conn = psycopg2.connect(
    "dbname=SpatialHomework user=postgres password=112361 host=localhost")
cur = conn.cursor()

# 文件夹路径
folder_path = "D:\\Project\\IDEAproject\\SpatialImageQuery\\img1"

for filename in os.listdir(folder_path):
    # 提取文件名中的数字
    match = re.match(r'(\d+)c(\d+)r(\d+)\.jpg', filename)
    if match:
        LEVEL, COL, ROW = match.groups()

        # 读取 JPG 文件的二进制数据
        with open(os.path.join(folder_path, filename), 'rb') as f:
            image_binary = f.read()
        # 插入数据到表中
        cur.execute("INSERT INTO \"RSImageQuery\" (\"LEVEL\", \"ROW\", \"COL\", \"IMG\") VALUES (%s, %s, %s, %s)",
                    (LEVEL, ROW, COL, psycopg2.Binary(image_binary)))

# 提交更改
conn.commit()

# 关闭连接
cur.close()
conn.close()
