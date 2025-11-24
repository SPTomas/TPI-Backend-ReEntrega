# Este es el Dockerfile para servicio-transporte

# 1. Imagen base: Usamos la que funcionó
FROM amazoncorretto:21

# 2. Argumento: Definimos el nombre del archivo JAR
ARG JAR_FILE=target/*.jar

# 3. Copia: Copiamos el JAR (la app) dentro de la imagen
COPY ${JAR_FILE} app.jar

# 4. Exponer: Le decimos a Docker que tu app corre en el puerto 8083
# (Coincide con tu server.port=8083)
EXPOSE 8083

# 5. Ejecutar: El comando para iniciar tu app
ENTRYPOINT ["java","-jar","/app.jar"]