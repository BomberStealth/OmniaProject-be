# Multi-stage build per ottimizzare l'immagine finale
FROM openjdk:17-jdk-slim AS builder

# Installa Maven
RUN apt-get update && apt-get install -y maven

# Crea directory di lavoro
WORKDIR /app

# Copia i file di configurazione Maven
COPY pom.xml .

# Scarica le dipendenze (layer cacheable)
RUN mvn dependency:go-offline -B

# Copia il codice sorgente
COPY src ./src

# Builda l'applicazione
RUN mvn clean package -DskipTests

# Stage finale - runtime
FROM openjdk:17-jre-slim

# Installa pigpio per Pi4J (necessario per GPIO su Raspberry Pi)
RUN apt-get update && \
    apt-get install -y pigpio && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Crea directory di lavoro
WORKDIR /app

# Copia il JAR dalla fase di build
COPY --from=builder /app/target/raspberry-controller-1.0.0.jar app.jar

# Espone la porta 3000
EXPOSE 3000

# Avvia pigpiod daemon e l'applicazione
CMD ["sh", "-c", "pigpiod && java -jar app.jar"]