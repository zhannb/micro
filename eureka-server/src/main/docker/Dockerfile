FROM java:8
RUN mkdir /microservice
WORKDIR /microservice
ADD /crawler-eureka.jar /microservice/
EXPOSE 7770
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "-Dspring.profiles.active=docker","/microservice/crawler-eureka.jar","> /docker/log/eureka.log &"]
