#!/bin/bash
npm run gulp -- build
lein cljs-build
HOST=0.0.0.0 \
BASE_URL=http://192.168.50.60:5000 \
lein run
