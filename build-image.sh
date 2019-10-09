#!/bin/bash

echo '============================================================='
echo '$                                                           $'
echo '$              crawler                                      $'
echo '$                                                           $'
echo '$                                                           $'
echo '$  email:    iamcrawler@sina.com                            $'
echo '$  homePage: http://www.iamcrawler.cn                       $'
echo '$                                                           $'
echo '$                                                           $'
echo '============================================================='
echo '.'

# if you have engine project . plz add
# cd ${your engine path}
# mvn clean install

cd eureka-server

mvn clean package docker:build

cd ..

cd config-server

mvn clean package docker:build

cd ..

cd zuul-server

mvn clean package docker:build

cd ..

cd auth-server

mvn clean package docker:build

cd ..


cd user-server

mvn clean package docker:build

cd ..

cd zipkin-server

mvn clean package docker:build

cd ..