# admin-service

[![Build and Push Docker Image](https://github.com/now-start/admin-service/actions/workflows/build.yaml/badge.svg)](https://github.com/now-start/admin-service/actions/workflows/build.yaml)

https://spring.nowstart.org

## dependency

- Security
    - spring-boot-starter-security
- Admin Page
    - spring-boot-starter-actuator
    - spring-boot-admin-starter-server
    - spring-cloud-starter-netflix-eureka-server

## Environment

| key           | required |
|---------------|----------|
| SERVICE_URL   |          |

## docker-compose

```
services:
  monitor:
    user: root
    restart: always
    image: ghcr.io/now-start/gateway-service:latest
    ports:
      - 18080:8080
    volumes:
      - ./log:/workspace/log
    environment:
      - TZ=Asia/Seoul
```
