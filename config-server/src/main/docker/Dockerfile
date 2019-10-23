FROM java:8
RUN mkdir /microservice
WORKDIR /microservice
ADD /crawler-config.jar /microservice/
EXPOSE 7771
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-jar","-Dspring.profiles.active=docker", "/microservice/crawler-config.jar","> /docker/log/config.log &"]