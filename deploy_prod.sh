#!/usr/bin/env bash

ssh $REMOTE_USER@$SERVER_IP <<EOF
  echo $REMOTE_PASSWORD | sudo docker stop stonecutter || echo 'Failed to stop stonecutter container'
  sudo docker rm stonecutter || echo 'Failed to remove stonecutter container'
  sudo docker rmi dcent/stonecutter
  sudo docker run -d -v /var/stonecutter/config:/var/config \
             --env-file=/var/stonecutter/config/stonecutter.env \
             -v /data/stonecutter/static \
             -v /var/stonecutter/email_service \
             -p 5000:5000 \
             --restart=on-failure \
             --link mongo:mongo --name stonecutter dcent/stonecutter
EOF