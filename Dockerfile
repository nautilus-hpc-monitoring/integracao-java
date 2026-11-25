FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/monitoramento-alertas-1.0-SNAPSHOT.jar /app/app.jar
ENV TZ=America/Sao_Paulo
ENTRYPOINT ["java", "-Duser.timezone=America/Sao_Paulo", "-cp", "/app/app.jar"]
