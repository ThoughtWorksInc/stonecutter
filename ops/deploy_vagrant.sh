#!/bin/bash
ssh vagrant@192.168.50.61 <<EOF
  sudo docker stop stonecutter || echo 'Failed to stop stonecutter container'
  sudo docker rm stonecutter || echo 'Failed to remove stonecutter container'
  sudo docker run -d -v /var/stonecutter/target:/var/stonecutter -v /var/stonecutter/config/clients.yml:/var/config/clients.yml -p 5000:5000 --name stonecutter --link mongo:mongo "--env-file=/var/stonecutter/config/stonecutter.env" java:8 bash -c 'java -jar /var/stonecutter/*standalone.jar'
EOF
