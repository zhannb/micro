FROM java:8
RUN mkdir /microservice
WORKDIR /microservice
ADD /crawler-zuul.jar /microservice/
EXPOSE 7772
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-jar","-Dspring.profiles.active=docker", "/microservice/crawler-zuul.jar","> /docker/log/zuul.log &"]