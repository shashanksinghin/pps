FROM openjdk:17
WORKDIR /app
COPY ./target/PPSPoC-0.0.1-SNAPSHOT.jar ./app.jar
COPY ./src/scripts/startApp.sh startApp.sh
EXPOSE 8082

ENTRYPOINT ["./startApp.sh"]
