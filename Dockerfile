# Imagen de la aplicación Uruguay Mnatural. Se arma en dos etapas:
#   1) "build": compila el proyecto con Maven y genera el .jar
#   2) la imagen final: solo Java (sin Maven ni el código fuente) y el .jar, así queda más liviana

# ---------- Etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Primero solo el pom.xml: Docker guarda las dependencias descargadas y no las vuelve a bajar
# mientras el pom no cambie (los siguientes builds son mucho más rápidos)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

# Después el código, y se arma el .jar (los tests se corren aparte, desde IntelliJ)
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# Carpeta donde se guardan las fotos que suben los usuarios (en docker-compose se conecta a ./uploads)
RUN mkdir -p /app/uploads

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
