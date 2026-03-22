FROM eclipse-temurin:21-jre-alpine

RUN apk upgrade --no-cache

WORKDIR /app

COPY target/dude.jar dude.jar
COPY languages.yml languages.yml
COPY .ignore .ignore
COPY config.txt config.txt

RUN mkdir -p results logs

ENTRYPOINT ["java", "-Xmx8g", "-jar", "dude.jar"]
