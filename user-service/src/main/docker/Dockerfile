FROM java:8
RUN mkdir /microservice
WORKDIR /microservice
ADD /crawler-user.jar /microservice/
EXPOSE 7775
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-jar","-Dspring.profiles.active=docker", "/microservice/crawler-user.jar","> /docker/log/user.log &"]