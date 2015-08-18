#!/bin/bash

ssh $REMOTE_USER@$SERVER_IP "mkdir -p /var/stonecutter/target"
ssh $REMOTE_USER@$SERVER_IP "mkdir -p /var/stonecutter/config"
ssh $REMOTE_USER@$SERVER_IP "mkdir -p /data/stonecutter/static"
scp target/*-standalone.jar $REMOTE_USER@$SERVER_IP:/var/stonecutter/target/stonecutter-standalone.jar
scp stonecutter_$SNAP_STAGE_NAME.env $REMOTE_USER@$SERVER_IP:/var/stonecutter/config/stonecutter.env
scp rsa-keypair_$SNAP_STAGE_NAME.json $REMOTE_USER@$SERVER_IP:/var/stonecutter/config/rsa-keypair.json
scp clients.yml $REMOTE_USER@$SERVER_IP:/var/stonecutter/config/clients.yml
scp logo.svg $REMOTE_USER@$SERVER_IP:/data/stonecutter/static/logo.svg
scp dcent-favicon.ico $REMOTE_USER@$SERVER_IP:/data/stonecutter/static/dcent-favicon.ico
ssh $REMOTE_USER@$SERVER_IP <<EOF
  sudo docker stop stonecutter || echo 'Failed to stop stonecutter container'
  sudo docker rm stonecutter || echo 'Failed to remove stonecutter container'
  sudo docker run -d -v /var/stonecutter/target:/var/stonecutter \
                     -v /var/stonecutter/config/clients.yml:/var/config/clients.yml \
                     -v /var/stonecutter/config/rsa-keypair.json:/var/config/rsa-keypair.json \
                     -v /data/stonecutter/static:/data/stonecutter/static \
                     -v /var/stonecutter/email_service:/var/stonecutter/email_service \
                     -p 127.0.0.1:5000:3000 --name stonecutter --link mongo:mongo \
                     --env-file=/var/stonecutter/config/stonecutter.env \
                     java:8 bash -c 'java -Dlog4j.configuration=log4j.dev -jar /var/stonecutter/stonecutter-standalone.jar'
EOF
