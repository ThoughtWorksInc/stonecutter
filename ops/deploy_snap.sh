#!/bin/bash

ssh $REMOTE_USER@$SERVER_IP "mkdir -p /var/stonecutter/config"
ssh $REMOTE_USER@$SERVER_IP "mkdir -p /data/stonecutter/static"
scp stonecutter_$SNAP_STAGE_NAME.env $REMOTE_USER@$SERVER_IP:/var/stonecutter/config/stonecutter.env
scp rsa-keypair_$SNAP_STAGE_NAME.json $REMOTE_USER@$SERVER_IP:/var/stonecutter/config/rsa-keypair.json
scp clients.yml $REMOTE_USER@$SERVER_IP:/var/stonecutter/config/clients.yml
scp logo.svg $REMOTE_USER@$SERVER_IP:/data/stonecutter/static/logo.svg
scp dcent-favicon.ico $REMOTE_USER@$SERVER_IP:/data/stonecutter/static/dcent-favicon.ico
ssh $REMOTE_USER@$SERVER_IP <<EOF
  sudo docker stop stonecutter || echo 'Failed to stop stonecutter container'
  sudo docker rm stonecutter || echo 'Failed to remove stonecutter container'
  sudo docker run -d -v /var/stonecutter/config:/var/config \
             --env-file=/var/stonecutter/config/stonecutter.env \
             -v /data/stonecutter/static:/data/stonecutter/static \
             -v /var/stonecutter/email_service:/var/stonecutter/email_service \
             -p 127.0.0.1:5000:5000 \
             --link mongo:mongo --name stonecutter dcent/stonecutter
EOF
