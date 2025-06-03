FROM openjdk:17
WORKDIR /app
COPY ./target/PPSPoC-0.0.1-SNAPSHOT.jar ./app.jar
COPY ./src/scripts/startApp.sh startApp.sh
COPY ./src/scripts/startApp.sh setEnv.sh
RUN exec chmod 755 startApp.sh
RUN exec chmod 755 setEnv.sh
EXPOSE 8082:8082

ENTRYPOINT ["./startApp.sh"]
#CMD ["sh", "-c", "tail -f /dev/null"]