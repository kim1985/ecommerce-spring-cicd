# Dockerfile per E-commerce Spring Boot Application

# Usa immagine base con Java 17 già installato
# Alpine è una distribuzione Linux leggera, perfetta per container
FROM eclipse-temurin:17-jre-alpine

# Imposta la directory di lavoro dentro il container
# Tutti i comandi successivi verranno eseguiti in /app
WORKDIR /app

# Copia il file JAR dal target directory (creato da Maven)
# dentro il container nella cartella /app
COPY target/*.jar app.jar

# Dichiara che il container userà la porta 8080
# Questa è la porta standard di Spring Boot
EXPOSE 8080

# Comando che viene eseguito quando il container si avvia
# Avvia l'applicazione Java con il JAR che abbiamo copiato
CMD ["java", "-jar", "app.jar"]