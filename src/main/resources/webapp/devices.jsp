<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="java.lang.Package" %>

<html>
    <head>
        <meta http-equiv="x-ua-compatible" content="IE=9" />
        <title>Сервер ККТ</title>
        <link rel="stylesheet" href="static/css/bootstrap.min.css">
            <script src="static/js/jquery-1.11.3.min.js"></script>
            <script src="static/js/bootstrap.min.js"></script>
    </head>
    <body class="container-fluid">
        <div class="container">
            <div id="app" class="row">
                <div class="col-12">
                    <nav id="nav" class="navbar navbar-expand-sm navbar-light bg-light">
                        <a href="#/" class="navbar-brand active">ДТО WEB-сервер</a>
                        <button type="button" data-toggle="collapse" data-target="#mainNavBar" aria-controls="mainNavBar" aria-expanded="false" aria-label="Toggle navigation" class="navbar-toggler">
                            <span class="navbar-toggler-icon"></span>
                        </button>
                        <div id="mainNavBar" class="collapse navbar-collapse">
                            <div class="navbar-nav">
                                <a href="#/devices" class="nav-item nav-link active">Устройства</a>
                                <a href="#/settings" class="nav-item nav-link">Настройки web-сервера</a>
                            </div></div>
                    </nav>
                </div>
                <div class="col-12">
                    <div data-v-d484a782="" class="row">
                        <div data-v-d484a782="" class="col-12 pt-3">
                            <div data-v-d484a782="" class="devices"><div data-v-d484a782="" class="row">
                                    <div data-v-d484a782="" class="col-8">
                                        <h4 data-v-d484a782="">Устройства</h4>
                                    </div><div data-v-d484a782="" class="col-4 text-right">
                                        <button data-v-d484a782="" id="show-modal" class="btn btn-primary btn-sm">
                                            <svg data-v-d484a782="" aria-hidden="true" focusable="false" data-prefix="fas" data-icon="plus" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512" class="svg-inline--fa fa-plus fa-w-14">
                                                <path data-v-d484a782="" fill="currentColor" d="M416 208H272V64c0-17.67-14.33-32-32-32h-32c-17.67 0-32 14.33-32 32v144H32c-17.67 0-32 14.33-32 32v32c0 17.67 14.33 32 32 32h144v144c0 17.67 14.33 32 32 32h32c17.67 0 32-14.33 32-32V304h144c17.67 0 32-14.33 32-32v-32c0-17.67-14.33-32-32-32z" class=""></path>
                                            </svg> Добавить </button>
                                    </div>
                                </div>
                                <div data-v-d484a782="" class="table-responsive">
                                    <div data-v-d484a782="" role="alert" class="alert alert-info"><h4 data-v-d484a782="" class="alert-heading">Отсутствуют...</h4><p data-v-d484a782="">Для отображения списка устройств необходимо для начала их добавить...</p>
                                        <hr data-v-d484a782="">
                                            <p data-v-d484a782="" class="mb-0">Для добавления устройства, нажмите на соответствующую кнопку <button data-v-d484a782="" id="show-modal-2" class="btn btn-primary btn-sm">
                                                    <svg data-v-d484a782="" aria-hidden="true" focusable="false" data-prefix="fas" data-icon="plus" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512" class="svg-inline--fa fa-plus fa-w-14">
                                                        <path data-v-d484a782="" fill="currentColor" d="M416 208H272V64c0-17.67-14.33-32-32-32h-32c-17.67 0-32 14.33-32 32v144H32c-17.67 0-32 14.33-32 32v32c0 17.67 14.33 32 32 32h144v144c0 17.67 14.33 32 32 32h32c17.67 0 32-14.33 32-32V304h144c17.67 0 32-14.33 32-32v-32c0-17.67-14.33-32-32-32z" class=""></path>
                                                    </svg> Добавить </button> , заполните обязательные поля и в модульном окне нажмите кнопку "Добавить" </p>
                                            <figure data-v-d484a782="" class="figure mt-3">
                                                <img data-v-d484a782="" alt="Пример модульного окна" src="/img/add_device_modal.80da6c4c.png" class="figure-img img-fluid rounded">
                                                    <figcaption data-v-d484a782="" class="figure-caption text-right">Пример модульного окна.</figcaption>
                                            </figure>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div data-v-37eb2038="" data-v-d484a782="" id="showModal_9" tabindex="-1" role="dialog" aria-hidden="true" data-backdrop="static" data-keyboard="false" class="modal">
                            <div data-v-37eb2038="" role="document" class="modal-dialog modal-dialog-centered">
                                <div data-v-37eb2038="" class="modal-content">
                                    <div data-v-37eb2038="" class="modal-header">
                                        <h5 data-v-37eb2038="" id="exampleModalLongTitle" class="modal-title">Добавление нового устройства</h5>
                                    </div>
                                    <div data-v-37eb2038="" class="modal-body">
                                        <form data-v-37eb2038="" novalidate="novalidate" class="needs-validation">
                                            <div data-v-37eb2038="" class="form-row">
                                                <div data-v-37eb2038="" class="col-12 mb-3">
                                                    <label data-v-37eb2038="" for="deviceId">Уникальный идентификатор ККТ</label>
                                                    <input data-v-37eb2038="" type="text" id="deviceId" placeholder="Идентификатор устройства" pattern="^[a-zA-Z0-9_-]{1,16}$" value="Mark" required="required" class="form-control">
                                                        <div data-v-37eb2038="" class="invalid-feedback"> Поле обязательно для заполнения, должно состоять из цифр, букв латинского алфавита любого регистра, нижнего подчеркивания и тире, длина идентификатара от 1 до 16 символов. </div>
                                                </div>
                                                <div data-v-37eb2038="" class="col-12 mb-3">
                                                    <label data-v-37eb2038="" for="deviceName">Название ККТ</label>
                                                    <input data-v-37eb2038="" type="text" id="deviceName" placeholder="Произвольное название устройства" pattern="^.{0,128}$" class="form-control">
                                                        <div data-v-37eb2038="" class="invalid-feedback"> Имя устройства должно быть не более 128 символов </div>
                                                </div>

                                            </div>
                                        </form><!---->
                                    </div>
                                    <div data-v-37eb2038="" class="modal-footer">
                                        <button data-v-37eb2038="" type="button" class="btn btn-primary">Добавить</button>
                                        <button data-v-37eb2038="" type="button" class="btn btn-secondary">Отмена</button></div>
                                </div>
                            </div>
                        </div>
                        <div data-v-85fdf2fe="" data-v-d484a782="" id="showModal_10" tabindex="-1" role="dialog" aria-hidden="true" class="modal fade">
                            <div data-v-85fdf2fe="" role="document" class="modal-dialog">
                                <div data-v-85fdf2fe="" class="modal-content">
                                    <div data-v-85fdf2fe="" class="modal-header">
                                        <h5 data-v-85fdf2fe="" id="exampleModalLabel" class="modal-title">Подтвердите действие</h5>
                                    </div>
                                    <div data-v-85fdf2fe="" class="modal-body">
                                    </div>
                                    <div data-v-85fdf2fe="" class="modal-footer">
                                        <button data-v-85fdf2fe="" type="button" class="btn btn-danger"> Удалить </button>
                                        <button data-v-85fdf2fe="" type="button" class="btn btn-secondary"> Отмена </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div id="toaster-container" aria-live="polite" aria-atomic="true">
                    <div id="toaster" style="position: fixed; top: 10px; right: 10px; width: 250px;"></div>
                </div>
                <div class="col-12 pt-5">
                    <div class="navbar navbar-light bg-light">
                        <span class="text-muted">Версия WEB-сервера: <b>10.8.1.0</b></span>
                        <span class="text-muted">Версия подключенного драйвера: <b>10.8.1.0</b></span>
                    </div>
                </div>
            </div>
        </div>
    </body>
</html>