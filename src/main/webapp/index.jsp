<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>查询影像块</title>
</head>
<body>
<script>
    // 页面加载时执行
    window.onload = function() {
        // 获取uploadSuccess属性的值
        var uploadSuccess = <%= request.getAttribute("uploadSuccess") %>;
        // 如果uploadSuccess为true，则显示成功消息
        if (uploadSuccess) {
            alert("上传成功！");
        }
    };
</script>

<h1>查询影像块</h1>
<form action="pic" method="get">
    <label for="level">级别（L）:</label>
    <input type="text" id="level" name="L"><br><br>
    <label for="row">行号（R）:</label>
    <input type="text" id="row" name="R"><br><br>
    <label for="col">列号（C）:</label>
    <input type="text" id="col" name="C"><br><br>
    <input type="submit" value="查询">
</form>

<form action="upload" method="post" enctype="multipart/form-data">
    选择文件：<input type="file" name="file" /><br/><br/>
    <input type="submit" value="上传" />
</form>

<button onclick="redirectToMapLayer()">进入MapLayer</button>

<script>
    function redirectToMapLayer() {
        window.location.href = 'MapLayer.html';
    }
</script>

</body>
</html>
