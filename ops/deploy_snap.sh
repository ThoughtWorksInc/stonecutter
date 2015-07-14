#!/bin/bash

ssh $REMOTE_USER@$SERVER_IP "mkdir -p /var/stonecutter/target"
ssh $REMOTE_USER@$SERVER_IP "mkdir -p /var/stonecutter/config"
scp target/*-standalone.jar $REMOTE_USER@$SERVER_IP:/var/stonecutter/target/stonecutter-standalone.jar
scp stonecutter.env $REMOTE_USER@$SERVER_IP:/var/stonecutter/config/stonecutter.env
./deploy.sh
# Steps
 # scp app onto digital ocean
 # scp config file onto digital ocean
 # run deployment script
