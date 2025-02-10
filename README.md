# АТОЛ. Web-сервер драйвера ККТ v.10

## Настройки

### Файл проекта `application.properties`:
```
web.port # порт сервера
```

### Переменные jvm:
```
db.directory # расположение каталога БД 'web.s3db'
log.directory # расположение каталога журналов 'web.log'
```

## Пример запуска

```java
java -Ddb.directory=D:\ATOL -Dlog.directory=D:\ATOL -jar AtolWebServer.jar
```
