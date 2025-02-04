<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="java.lang.Package" %>

<html>
    <head>
        <meta http-equiv="x-ua-compatible" content="IE=9">
        <title>Сервер ККТ</title>
        <link rel="stylesheet" href="static/css/bootstrap.min.css">
        <script src="static/js/jquery-1.11.3.min.js"></script>
        <script src="static/js/bootstrap.min.js"></script>
    </head>
    <body class="container-fluid">
        <div id="body-div">
            <div class="row">
                <div class="col-md-8">
                    <div class="form-group">
                        <p>Версия сервера ККТ: ${serverVersion}</p>
                        <p>Версия драйвера: ${driverVersion}</p>
                    </div>
                </div>
            </div>
        </div>
    </body>
</html>