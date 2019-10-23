FROM java:8
RUN mkdir /microservice
WORKDIR /microservice
ADD /crawler-auth.jar /microservice/
EXPOSE 7774
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-jar","-Dspring.profiles.active=docker", "/microservice/crawler-auth.jar","> /docker/log/auth.log &"]