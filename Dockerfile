FROM dcent/clojure-with-npm:0

RUN mkdir /usr/src/app
WORKDIR /usr/src/app

COPY project.clj /usr/src/app/
RUN lein uberjar
COPY target/*standalone.jar /usr/src/app


CMD ["java", "-jar", "stonecutter-0.1.0-SNAPSHOT-standalone.jar"]
