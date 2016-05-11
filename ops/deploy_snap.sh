#!/bin/bash

ssh $REMOTE_USER@$SERVER_IP "mkdir -p /var/stonecutter/config"
ssh $REMOTE_USER@$SERVER_IP "mkdir -p /data/stonecutter/static"
scp stonecutter_$SNAP_STAGE_NAME.env $REMOTE_USER@$SERVER_IP:~/stonecutter.env
scp rsa-keypair_$SNAP_STAGE_NAME.json $REMOTE_USER@$SERVER_IP:~/rsa-keypair.json
scp clients.yml $REMOTE_USER@$SERVER_IP:~/clients.yml
scp logo.svg $REMOTE_USER@$SERVER_IP:~/logo.svg
scp dcent-favicon.ico $REMOTE_USER@$SERVER_IP:~/dcent-favicon.ico
ssh $REMOTE_USER@$SERVER_IP <<EOF
  echo $REMOTE_PASSWORD | sudo -S mv ~/stonecutter.env /var/stonecutter/config/stonecutter.env
  sudo mv ~/rsa-keypair.json /var/stonecutter/config/rsa-keypair.json
  sudo mv ~/clients.yml /var/stonecutter/config/clients.yml
  sudo mv ~/logo.svg /data/stonecutter/static/logo.svg
  sudo mv ~/dcent-favicon.ico /data/stonecutter/static/dcent-favicon.ico
  sudo docker stop stonecutter || echo 'Failed to stop stonecutter container'
  sudo docker rm stonecutter || echo 'Failed to remove stonecutter container'
  sudo docker rmi dcent/stonecutter
  sudo docker run -d -v /var/stonecutter/config:/var/config \
             --env-file=/var/stonecutter/config/stonecutter.env \
             -v /data/stonecutter/static:/data/stonecutter/static \
             -v /var/stonecutter/email_service:/var/stonecutter/email_service \
             -p 5000:5000 \
             --restart=on-failure \
             --link mongo:mongo --name stonecutter dcent/stonecutter
EOF
