FROM hub.c.163.com/wuxukun/maven-aliyun:3-jdk-8

ADD pom.xml /tmp/build/

ADD src /tmp/build/src

RUN cd /tmp/build && mvn clean package \
        #拷贝编译结果到指定目录
        && mv target/*.war /crawler-eureka.war \
        #清理编译痕迹
        && cd / && rm -rf /tmp/build

VOLUME /tmp
EXPOSE 8181
ENTRYPOINT ["java","-jar","/crawler-eureka.war"]
