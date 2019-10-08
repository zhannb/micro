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

cd crawler-config

mvn clean package docker:build

cd ..

cd crawler-zuul

mvn clean package docker:build

cd ..

cd crawler-auth

mvn clean package docker:build

cd ..
