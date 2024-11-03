FROM maven:3.8.6-openjdk-11

WORKDIR /app
COPY . /app

RUN mvn clean install

# Keep the container alive with a shell
CMD ["tail", "-f", "/dev/null"]