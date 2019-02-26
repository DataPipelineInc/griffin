#!/bin/bash
set -e

echo "begin compiler jar ..."
#mvn clean install

container_id=`docker ps |grep apachegriffin/griffin_spark2:0.3.0|awk '{print $1}'`
echo "griffin container id is ${container_id}"
docker cp ../service/target/service-0.5.0-dp-SNAPSHOT.jar ${container_id}:/root/service/service.jar
echo "cp service jar into docker successful"
docker cp ../measure/target/measure-0.5.0-dp-SNAPSHOT.jar ${container_id}:/root/measure/griffin-measure.jar
echo "cp measure jar into docker successful"