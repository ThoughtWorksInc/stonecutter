#!/usr/bin/env bash
start-stop-daemon --start -b -x /usr/bin/Xvfb -- :1 -screen 0 1280x1024x16
DISPLAY=:1 lein do clean, midje $*
start-stop-daemon --stop -x /usr/bin/Xvfb