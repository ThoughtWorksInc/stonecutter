#!/bin/bash

ssh $REMOTE_USER@$SERVER_IP "mkdir -p /var/stonecutter/target"
scp target/*-standalone.jar $REMOTE_USER@$SERVER_IP:/var/helsinki/dist/stonecutter.jar
./deploy.sh
# Steps
 # scp app onto digital ocean
 # scp config file onto digital ocean
 # run deployment script
