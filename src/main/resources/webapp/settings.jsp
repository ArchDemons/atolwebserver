<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="ru.atol.drivers10.webserver.settings.SelectItem" %>
<%@ page import="java.util.List" %>

<html>
    <head>
        <meta http-equiv="x-ua-compatible" content="IE=9">
        <title>Настройки сервера</title>
        <link rel="stylesheet" href="static/css/bootstrap.min.css">
        <script src="static/js/jquery-1.11.3.min.js"></script>
        <script src="static/js/bootstrap.min.js"></script>
    </head>
    <body class="container-fluid">
        <div id="body-div">
            <div class="row">
                <div class="col-md-8">
                    <div class="form-group">
                        <form method="POST" action="settings">

                            <div class="panel-group" id="accordion">

                                <div class="panel panel-default">
                                    <div class="panel-heading">
                                        <h4 class="panel-title">
                                            <a data-toggle="collapse" href="#common_settings" data-parent="#accordion">Общие настройки</a>
                                        </h4>
                                    </div>
                                    <div id="common_settings" class="panel-collapse collapse">
                                        <div class="panel-body">
                                            <input type="checkbox" id="isActive" name="isActive" value="isActive" ${isActive ? 'checked' : ''}>&nbsp;Активировать сервер
                                        </div>
                                    </div>
                                </div>

                                <div class="panel panel-default">
                                    <div class="panel-heading">
                                        <h4 class="panel-title">
                                            <a data-toggle="collapse" href="#device_settings" data-parent="#accordion">Настройки связи с ККТ</a>
                                        </h4>
                                    </div>
                                    <div id="device_settings" class="panel-collapse collapse">
                                        <div class="panel-body">
                                            <div class="form-group">
                                                <label for="channels">Канал обмена с ККТ:</label>
                                                <select id="channels" name="channels" class="form-control custom">
                                                <c:forEach var="channel" items="${channels}">
                                                    <option value="${channel.id}" ${channel.selected ? 'selected' : ''}>${channel.name}</option>
                                                </c:forEach>
                                                </select>
                                            </div>

                                            <div class="form-group">
                                                <label for="usbDevicePaths">USB:</label>
                                                <select id="usbDevicePaths" name="usbDevicePaths" class="form-control custom">
                                                <c:forEach var="usb" items="${usbDevicePaths}">
                                                    <option value="${usb.id}" ${usb.selected ? 'selected' : ''}>${usb.name}</option>
                                                </c:forEach>
                                                </select>
                                            </div>

                                            <div class="form-group">
                                                <label for="coms">COM-порт:</label>
                                                <select id="coms" name="coms" class="form-control custom">
                                                <c:forEach var="com" items="${coms}">
                                                    <option value="${com.id}" ${com.selected ? 'selected' : ''}>${com.name}</option>
                                                </c:forEach>
                                                </select>
                                            </div>

                                            <div class="form-group">
                                                <label for="baudrates">Скорость:</label>
                                                <select id="baudrates" name="baudrates" class="form-control custom">
                                                <c:forEach var="baudrate" items="${baudrates}">
                                                    <option value="${baudrate.id}" ${baudrate.selected ? 'selected' : ''}>${baudrate.name}</option>
                                                </c:forEach>
                                                </select>
                                            </div>

                                            <label for="tcpAddress">IP-адрес</label>
                                            <div class="row">
                                                <div class="form-group col-xs-6">
                                                    <input type="text" id="tcpAddress" name="tcpAddress" value="${tcpAddress}" class="form-control">
                                                </div>
                                                <div class="form-group col-xs-6">
                                                    <input type="text" id="tcpPort" name="tcpPort" value="${tcpPort}" class="form-control">
                                                </div>
                                            </div>

                                            <div class="form-group">
                                                <label for="ofdChannels">Канал до ОФД:</label>
                                                <select id="ofdChannels" name="ofdChannels" class="form-control custom">
                                                <c:forEach var="ofdChannel" items="${ofdChannels}">
                                                    <option value="${ofdChannel.id}" ${ofdChannel.selected ? 'selected' : ''}>${ofdChannel.name}</option>
                                                </c:forEach>
                                                </select>
                                            </div>

                                        </div>
                                    </div>
                                </div>
                            </div>

                            <input type="submit" value="Сохранить" class="btn btn-primary">
                        </form>

                    </div>
                </div>
            </div>
        </div>
    </body>
</html>