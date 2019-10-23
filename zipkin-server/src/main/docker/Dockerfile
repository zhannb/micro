FROM java:8
RUN mkdir /microservice
WORKDIR /microservice
ADD /crawler-zipkin.jar /microservice/
EXPOSE 7773
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-jar","-Dspring.profiles.active=docker", "/microservice/crawler-zipkin.jar","> /docker/log/zipkin.log &"]