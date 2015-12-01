FROM dcent/clojure-with-npm:0

RUN mkdir /usr/src/app
WORKDIR /usr/src/app

COPY project.clj /usr/src/app/
RUN lein with-profile production deps
COPY . /usr/src/app
RUN npm install --no-optional

CMD ["lein", "with-profile", "docker", "run"]
