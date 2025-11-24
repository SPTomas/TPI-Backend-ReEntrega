# 1. Imagen base: Usamos una imagen de Java 21
# (Usamos amazoncorretto como en tu otro Dockerfile)
FROM amazoncorretto:21

# 2. Argumento: Definimos el nombre del archivo JAR
# (Maven lo crea en 'target/servicio-logistica-0.0.1-SNAPSHOT.jar')
ARG JAR_FILE=target/*.jar

# 3. Copia: Copiamos el JAR (la app) dentro de la imagen
COPY ${JAR_FILE} app.jar

# 4. Exponer: Le decimos a Docker que la app corre en el puerto 8084
# (Coincide con server.port=8084)
EXPOSE 8084

# 5. Ejecutar: El comando para iniciar la app
ENTRYPOINT ["java","-jar","/app.jar"]