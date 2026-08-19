# Cloud File Storage

Многопользовательское облачное файловое хранилище на Java и Spring Boot.

Пользователь может зарегистрироваться, авторизоваться и работать со своими файлами и папками: загружать, скачивать, искать, перемещать, переименовывать и удалять ресурсы.

## Стек

* Java 21
* Spring Boot
* Spring Security + Spring Session
* PostgreSQL + Spring Data JPA
* Liquibase
* Redis
* MinIO (S3)
* Swagger / OpenAPI
* JUnit + Testcontainers
* Docker Compose
* React

## Возможности

* регистрация и авторизация пользователей;
* хранение сессий в Redis;
* загрузка файлов и папок;
* создание пустых директорий;
* просмотр содержимого директорий;
* поиск ресурсов;
* скачивание файлов и директорий в ZIP;
* переименование и перемещение;
* рекурсивное удаление;
* изоляция файлов между пользователями.

## Деплой

[Открыть приложение](http://159.194.201.149:8080/)

## API

Документация API доступна через Swagger UI:

[Swagger UI](http://159.194.201.149:8080/swagger-ui/index.html#/)

## Запуск

Создать `.env` с параметрами PostgreSQL, Redis и MinIO, например:

```propeties
POSTGRES_USER=postgres
POSTGRES_PASSWORD=mysecretpassword
POSTGRES_DB=mydatabase
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadminpassword
REDIS_PASSWORD=redispassword
```

и запустить командой
```bash
docker compose up --build -d
```

После запуска приложение будет доступно локально.

## Тестирование

Для интеграционного тестирования используются JUnit и Testcontainers с PostgreSQL и MinIO.
