#!/usr/bin/env bash
npm run gulp -- build
lein cljs-build
if [ -z "$DISPLAY" ]
  then
    start-stop-daemon --start -b -x /usr/bin/Xvfb -- :1 -screen 0 1280x1024x16
    DISPLAY=:1 lein do clean, midje stonecutter.browser.* $*
    start-stop-daemon --stop -x /usr/bin/Xvfb
  else
    lein do clean, midje stonecutter.browser.* $*
fi