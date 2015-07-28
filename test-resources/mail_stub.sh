#!/bin/bash

email_address=$1
subject=$2
body=$3

echo "{:email-address \""$email_address"\"" \
        " :subject \""$subject"\"" \
        " :body "$body"}" \
        > test-tmp/test-email.txt
