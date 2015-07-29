#!/bin/bash

ssh $REMOTE_USER@$SERVER_IP "mkdir -p /var/stonecutter/target"
ssh $REMOTE_USER@$SERVER_IP "mkdir -p /var/stonecutter/config"
scp target/*-standalone.jar $REMOTE_USER@$SERVER_IP:/var/stonecutter/target/stonecutter-standalone.jar
scp stonecutter.env $REMOTE_USER@$SERVER_IP:/var/stonecutter/config/stonecutter.env
scp clients.yml $REMOTE_USER@$SERVER_IP:/var/stonecutter/config/clients.yml
ssh $REMOTE_USER@$SERVER_IP <<EOF
  sudo docker stop stonecutter || echo 'Failed to stop stonecutter container'
  sudo docker rm stonecutter || echo 'Failed to remove stonecutter container'
  sudo docker run -d -v /var/stonecutter/target:/var/stonecutter -v /var/stonecutter/config/clients.yml:/var/config/clients.yml -p 127.0.0.1:5000:3000 --name stonecutter --link mongo:mongo "--env-file=/var/stonecutter/config/stonecutter.env" java:8 bash -c 'java -jar /var/stonecutter/*standalone.jar'
EOF
