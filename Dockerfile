# ============================================================
# Build multi-estágio do Marmitas Sousa (Spring Boot 4 + Java 17)
#
# Etapa 1: compila o .jar com Maven + JDK 17.
# Etapa 2: roda apenas com o JRE 17 (imagem final enxuta).
# ============================================================

# ---------- Etapa 1: build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copia o pom primeiro para aproveitar o cache de dependências do Maven.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

# Copia o código-fonte e gera o jar executável.
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Roda como usuário sem privilégios (não root).
RUN groupadd -r app && useradd -r -g app app

# Pasta onde as fotos dos produtos são gravadas (vira volume no compose).
RUN mkdir -p /app/uploads && chown -R app:app /app

# Copia o jar da etapa de build.
# O nome segue <artifactId>-<version> do pom.xml (PDV-0.0.1-SNAPSHOT).
COPY --from=build /build/target/PDV-0.0.1-SNAPSHOT.jar app.jar

USER app
EXPOSE 8080

# Fuso horário do Brasil (importante para horários de funcionamento/impressão).
ENV TZ=America/Sao_Paulo

ENTRYPOINT ["java", "-Duser.timezone=America/Sao_Paulo", "-jar", "app.jar"]
