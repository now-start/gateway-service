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
| DISCOVERY_URL |          |

## docker-compose

```
services:
  monitor:
    user: root
    restart: always
    image: ghcr.io/now-start/admin-service:latest
    ports:
      - 8761:8761
    volumes:
      - ./log:/workspace/log
    environment:
      - TZ=Asia/Seoul
      - DISCOVERY_URL=http://eureka:8761/eureka
```
