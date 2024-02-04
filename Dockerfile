# syntax = docker/dockerfile:1.2
FROM clojure:openjdk-17 AS build

WORKDIR /
COPY . /

RUN clj -Sforce -T:build all

FROM azul/zulu-openjdk-alpine:17

COPY --from=build /target/stormdnd-standalone.jar /stormdnd/stormdnd-standalone.jar
COPY --from=build /resources /resources

EXPOSE $PORT

ENTRYPOINT exec java $JAVA_OPTS -jar /stormdnd/stormdnd-standalone.jar
