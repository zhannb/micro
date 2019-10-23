FROM java:8
RUN mkdir /microservice
WORKDIR /microservice
ADD /crawler-order.jar /microservice/
EXPOSE 7778
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-jar","-Dspring.profiles.active=docker", "/microservice/crawler-order.jar","> /docker/log/order.log &"]