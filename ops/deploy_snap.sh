#!/bin/bash

ssh $REMOTE_USER@$SERVER_IP "mkdir -p /var/stonecutter/target"
ssh $REMOTE_USER@$SERVER_IP "mkdir -p /var/stonecutter/config"
ssh $REMOTE_USER@$SERVER_IP "mkdir -p /data/stonecutter/static"
scp target/*-standalone.jar $REMOTE_USER@$SERVER_IP:/var/stonecutter/target/stonecutter-standalone.jar
scp stonecutter.env $REMOTE_USER@$SERVER_IP:/var/stonecutter/config/stonecutter.env
scp clients.yml $REMOTE_USER@$SERVER_IP:/var/stonecutter/config/clients.yml
scp logo.svg $REMOTE_USER@$SERVER_IP:/data/stonecutter/static/logo.svg
ssh $REMOTE_USER@$SERVER_IP <<EOF
  sudo docker stop stonecutter || echo 'Failed to stop stonecutter container'
  sudo docker rm stonecutter || echo 'Failed to remove stonecutter container'
  sudo docker run -d -v /var/stonecutter/target:/var/stonecutter -v /var/stonecutter/config/clients.yml:/var/config/clients.yml -v /data/stonecutter/static:/data/stonecutter/static -p 127.0.0.1:5000:3000 --name stonecutter --link mongo:mongo "--env-file=/var/stonecutter/config/stonecutter.env" java:8 bash -c 'java -jar /var/stonecutter/stonecutter-standalone.jar'
EOF
