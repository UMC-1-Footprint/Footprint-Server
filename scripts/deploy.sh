#!/usr/bin/env bash

REPOSITORY=/var/www/prod/deploy/footprint
cd $REPOSITORY

APP_NAME=footprint 
JAR_NAME=$(ls $REPOSITORY/build/libs/ | grep '.jar' | tail -n 1)
JAR_PATH=$REPOSITORY/build/libs/$JAR_NAME

CURRENT_PID=$(pgrep -f $APP_NAME)

echo "> BUILD START" >> /var/www/dev/deploy/footprint/footprintServerLog.out
sudo ./gradlew clean build >> /var/www/dev/deploy/footprint/footprintServerLog.out 2>&1
echo "> BUILD FINISH" >> /var/www/dev/deploy/footprint/footprintServerLog.out

# if [ -z $CURRENT_PID ] 
# then
#     echo "> 현재 구동중인 애플리케이션이 없으므로 종료하지 않습니다." >> /var/www/dev/deploy/footprint/footprintServerLog.out
# else
#   sudo kill -15 $CURRENT_PID
#   sleep 5
# fi

# echo "> $JAR_PATH 배포" >> /var/www/dev/deploy/footprint/footprintServerLog.out
# nohup java -jar $JAR_PATH > /var/www/dev/deploy/footprint/dev_footprint.log 2>&1 &
