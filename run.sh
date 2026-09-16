#!/bin/bash
export DB_URL="jdbc:mysql://localhost:3306/marmitassousa?useSSL=false&serverTimezone=UTC"
export DB_USER="root"
export DB_PASSWORD="123456"
mvn spring-boot:run
