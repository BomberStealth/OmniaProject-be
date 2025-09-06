# Multi-stage build per ottimizzare l'immagine finale
FROM eclipse-temurin:17-jdk AS builder

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
FROM eclipse-temurin:17-jre

# Installa gpiod tools per controllo GPIO
RUN apt-get update && \
    apt-get install -y \
        curl \
        gpiod \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Crea directory di lavoro
WORKDIR /app

# Copia il JAR dalla fase di build
COPY --from=builder /app/target/raspberry-controller-1.0.0.jar app.jar

# Espone la porta 3000
EXPOSE 3000

# Avvia l'applicazione
CMD ["java", "-jar", "app.jar"]