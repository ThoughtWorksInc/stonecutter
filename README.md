# stonecutter

[![Build Status](https://snap-ci.com/ThoughtWorksInc/stonecutter/branch/master/build_image)](https://snap-ci.com/ThoughtWorksInc/stonecutter/branch/master)

A D-CENT project: an easily deployable oauth server for small organisations.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Development VM

You can also develop and run the application in a VM.  You will need [Vagrant][] and [Ansible][] installed.

Navigate to the ops/ directory of the project and run:

    vagrant up

The first time this is run, it will provision and configure a new VM.

When the VM has started, access the virtual machine by running:

    vagrant ssh

The source folder will be located at `/var/stonecutter`

After initial setup you will need to run:

    cd /var/stonecutter
    npm install --no-bin-links

This will take a while (upwards of 5 minutes).

[Vagrant]: https://www.vagrantup.com
[Ansible]: http://docs.ansible.com/ansible/intro_installation.html

## Running

To start the app, run:

    ./manual_test_vm.sh

To start a web server for the application in development mode, run:

    lein ring server-headless

NB: running the application like this will save users into an in memory cache that will be destroyed as soon as the app is shutdown.

## Running test suite

Run all tests:

```
lein test
```

#### Clojure
Run all server (clojure) tests:

```
lein test-clj
```

Run just server unit tests:
```
lein unit
```
Run just server integration tests:
```
lein integration
```

Run just server browser tests:
```
lein browser
```

Autotest - Automatically run server tests on file changes (without browser tests):
```
lein auto-no-browser
```

#### Clojurescript
Run clojurescript tests:
```
lein test-cljs
```
Autotest - Automatically run clojurescript tests on file changes:
```
lein auto-cljs
```


## Running the static frontend

#### Getting started

First install [brew](http://brew.sh/)

```
brew install node
npm install
```

You also require gulp to be installed globally.

```
npm install -g gulp
```

Depending on system privileges you may need to install it globally with sudo:

```
sudo npm install -g gulp
```

#### Running the prototype

Simply type:

```
gulp server
```



## Customising the app

Optional environment variables can be set up to customise app name, colour scheme and logo.

#### App name

App name is used anywhere where the application refers to itself, e.g. "Register with {App name}".
To set the app name:

* Set the environment variable `APP_NAME`

The content of any HTML elements with the class `.clj--app-name` will be replaced with the app name.

#### Logo

Logo is used in the header. Maximum dimensions (W x H) 110px x 50px.
To set the logo:

* Set the environment variable `STATIC_RESOURCES_DIR_PATH` to a directory containing the logo.
* **NOTE: Anything inside this directory will be served as a static resource, including subdirectories.**
* Set the environment variable `LOGO_FILE_NAME` to the logo file name including the extension, e.g. logo.png

#### Favicon

Favicon should be an .ico file.
To set the favicon:

* Set the environment variable `STATIC_RESOURCES_DIR_PATH` to a directory containing the favicon
if you haven't already done so for the logo.
* **NOTE: Anything inside this directory will be served as a static resource, including subdirectories.**
* Set the environment variable `FAVICON_FILE_NAME` to the favicon file name, e.g. my-favicon.ico

#### Colours

The header colours can be customised:

* Set the environment variable `HEADER_BG_COLOR` to a CSS background-color value, e.g. `#1F1F1F` or `"rgb(192,192,192)"`
* Set the environment variable `HEADER_FONT_COLOR` to a CSS color value.
* Set the environment variable `HEADER_FONT_COLOR_HOVER` to a CSS color value.
* The font colours should contrast with the background colour for better visibility.

## Adding an email provider

Stonecutter can integrate against 3rd party email providers via a shell script interface, which may optionally require
configuration via environment variables.

Scripts for individual mail service providers should be located in the ```ops/roles/mail/files/providers``` directory.
These will be copied into a deployment environment by Ansible.  Currently, an implementation has been provided for
Mailgun, which can be used as a template.

In order to select which email service is used, set the ```EMAIL_SERVICE_PROVIDER``` environment variable.  This should
match one of the provider scripts under in the ```ops/roles/mail/files/providers``` directory.  For example, to use
the Mailgun provider:

    EMAIL_SERVICE_PROVIDER=mailgun

For the mailgun example, the following environment variables are also required:

- EMAIL_DOMAIN_NAME --- the domain name that has been linked to mailgun
- MAILGUN_API_KEY --- the mailgun api username + key (i.e. a string in the form: "api:_api-key_"), provided by Mailgun.

## Adding an Admin

A single admin user can be added on deployment, simply set two environment variables as follow.

- ADMIN_LOGIN --- this needs to be an email address, same format and validations apply as a normal login
- ADMIN_PASSWORD --- same format and validations apply just like a password for a user

## Architecture

The Continuous Delivery build and deployment architecture is documented [here] (https://docs.google.com/a/thoughtworks.com/drawings/d/1FZ35v27_pBym_NqzLbqVP_TwnHVBHNwFQss_Lzbs1bU/edit?usp=sharing).

The Hosting Architecture is documented [here] (https://docs.google.com/a/thoughtworks.com/drawings/d/1mgdxe0Q0uYZloZLFLvlwyKRx4iUangEAn4aV2qm-zWs/edit?usp=sharing).

## Deployment

### Deploying the application using docker
  
You can deploy the application using Docker. To do so, you will need three containers:
Mongo, Nginx and Stonecutter.

#### Starting a mongo container

To start a mongo container, run 

    docker run --name mongo mongo
    
#### Starting an Nginx container

To access the application you must run a reverse proxy, for example Nginx, that redirects to it, adding the following to the headers
    
    "X-Real-IP: <proxy ip>" 
    "X-Forwarded-For: <proxy ip>"
    "X-Forwarded-Proto: https"

To use Nginx for this you need 

* an SSL certificate and key
* a dhparam.pem file
* an nginx.conf file

You can acquire an SSL certificate and key online inexpensively. You should receive a pair of files, for instance stonecutter.crt and stonecutter.key. Store them in their own directory somewhere safe.

You can generate a dhparam.pem file by running 
    
    openssl dhparam -rand – 2048

In the terminal. After a while this will produce some text that looks like this:
    
    -----BEGIN DH PARAMETERS-----
    MIIBCAKCAQEAq9IcgRxcJukySS6UYDpluiwXJAUhsMCpz3vSvCLT9lrouraVy3kx
    ZJGRcLis2mxulTLOBNhhDuqISYNOjpec2Y70kPgY1R0Ydb4wANezcIK63bZi31ya
    1Tnd3ocRxLTSh4876kahzU0k63RaPBMtmhamK6CZcvHSSdFGcreVCgQmRhIshj88
    aHy9NDa6tHzCYBFj8AhI275cXOPGqE3pBFz+18A0Dc21xFJcHhM0a24cpyeMgY4h
    8IhS80lSg+ZLOhXoUiEWXV7AyeDzvIRc+6nR7MKq+CAzFHh3clvf3G62gW0a4Bpn
    sN/0EjRW0kjc2wF8bQ9933zv4P2Uoz0KewIBAg==
    -----END DH PARAMETERS-----

Create a new file dhparam.pem and paste this in.
 
You can create an nginx.conf file by copying the following into a new file and replacing the <> appropriately:

    events {
    }
    http {
      server {
        listen 80;
        return 301 $request_uri;
      }
      server {
        listen 443 ssl;
        ssl_certificate /etc/nginx/ssl/<file name for SSL certificate>;
        ssl_certificate_key /etc/nginx/ssl/<file name for SSL key>;
    
        ssl_session_cache shared:SSL:32m;
        ssl_session_timeout 10m;
    
        ssl_dhparam /etc/nginx/cert/dhparam.pem;
        ssl_protocols TLSv1.2 TLSv1.1 TLSv1;
    
        location / {
          proxy_pass http://<docker ip>:5000;
          proxy_set_header Host $host;
          proxy_set_header X-Real-IP $remote_addr;
          proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
          proxy_set_header X-Forwarded-Proto $scheme;
        }
      }
    }

Finally, run the following command:

    docker run -v <path to SSL certificates and keys directory>:/etc/nginx/ssl -v <path to conf file>/nginx.conf:/etc/nginx/nginx.conf -v <path to dhparam file>>/dhparam.pem:/etc/nginx/cert/dhparam.pem -P -d --name nginx-conainer nginx
        
#### Starting a Stonecutter container

To run Stonecutter you need 
 
* a clients.yml file 
* an rsa-keypair.json 
* a stonecutter.env file

To make a clients.yml file, copy the default one in this project, under Stonecutter/config. It shows the format used by the application.

To get an rsa keypair, see [below](#Adding public-private keypair for OpenID Connect).

Store both of these two files in their own directory.

To get a stonecutter.env, copy the template that is aso found in the config folder.
 
Finally, run this command, replacing <config file path> with the absolute path for the directory storing your config files, and <env file path> with the path to wherever your environment variable file is stored.  

    docker run -v <config file path>:/var/config --env-file=<env file path> -p 5000:5000 --link mongo:mongo --name stonecutter dcent/stonecutter
    
Adding public-private keypair for OpenID Connect
------------------------------------------------

To generate a public-private keypair in Json Web-key (JWK) format, enter the following at the command line:

```
> lein gen-keypair <key-id>
```

where the key-id is a custom identifier for the key (for example "20150824-stonecutter").

This will generate output similar to the following:

    JWK public key for client:
    ==========================
    {"kty":"RSA","kid":"20150824-stonecutter","n":"m38hDunIOBX4DdalnuoNoT7nVdn5gXprVFUlBX3KbDIwyKznX2QZQLDn_4_b94UsYlh1Vf33pO9TO9tsj2Hf1WdFQO72WqFUxFOk3ITc7OTc7p5oZhWYXsKCJh5dLl9G4tOxZ_vD-frD7c0M_-IUWQ9cuk7XulDNJqzHKSEHvbugokw-vOb9fI2CtBU9HtWHkbe3e8cJdbEN4zD7Qw7BrG5zCENGuWIMpe9XIpZTM0jwiclxNacNhU_eOiRk9wg7hHovGqFuSU8x0oohtaNe91YUCJsfnmHQZTARc8tGJOwhx4A8VAUnqVmmm7GCGx0CqvbzRtFolTbn39m3jMTtoQ","e":"AQAB"}


    JWK including private key for stonecutter:
    ==========================================
    {"kty":"RSA","kid":"20150824-stonecutter","n":"m38hDunIOBX4DdalnuoNoT7nVdn5gXprVFUlBX3KbDIwyKznX2QZQLDn_4_b94UsYlh1Vf33pO9TO9tsj2Hf1WdFQO72WqFUxFOk3ITc7OTc7p5oZhWYXsKCJh5dLl9G4tOxZ_vD-frD7c0M_-IUWQ9cuk7XulDNJqzHKSEHvbugokw-vOb9fI2CtBU9HtWHkbe3e8cJdbEN4zD7Qw7BrG5zCENGuWIMpe9XIpZTM0jwiclxNacNhU_eOiRk9wg7hHovGqFuSU8x0oohtaNe91YUCJsfnmHQZTARc8tGJOwhx4A8VAUnqVmmm7GCGx0CqvbzRtFolTbn39m3jMTtoQ","e":"AQAB","d":"kRSlaH-xorrErUy3TLU-MFM7jnuI80igOZgTqbL7GcYehC3m1rbTZOtqGqVD7AaiKcQ0_h2uYII3m6KYAJOmPztSf0o2KstaBq-wI1wHsTO7-xtrdsvxVYCP5DbyY-Dbh6lSXh2mdWeGRSrLVTfAGnRd5SrI1vqq3snYLMS3r0qSubpVjo1yGjcOitxgJWgvdRq2FRPplgRlnoaiMd5jVCNXvSP-2XXeIQq0nz_GLcqcjOI0hqPsEPFcdjtL9PdwXa7v3cmrjOcWprlFzBQVTL6YvT_kCKIghJsG9ksJoUzTafHUAYUBdfgQSTi0q-kommHr3SyQhL1aN4Khqm3jLQ","p":"9wOYB-B7mhbGsxh7qago75DqUhp3L2x56yP1pYA2dV0TBNQz2jlGjAJ-xzMCQ-AMOpGNtzWJ28A-aDcUo1ZXIam3qktCha38fIAuvgKR7k0tnjhLawIONBaA-OlSorszlAWdHJ3_4ckn0c_u9Zne0SHkQESJNY7ES23-Sca3AL8","q":"oSc_HO3y61wgMUDDTMtMFYaJA9UdO4fIEfEyu46VvgvIN2kvf2ayHTb01Pk-XsoL2OJUcmjg4g19sBt8xGCRU8as4DOBHb22rbYQ7qTa4ewTtqLQBTnrTMzWZLYN2JYCZydFCW63z9zypC34Uoi_AF-teDprNY-eepRkr9JbSZ8","dp":"KDhGlenAVmuk-N5grFQ8Lh3LeYjjpS4lf9sAEW2Z8GwyP5QJyVuQGBYD7I1qrgCaHSM8DvvBsa1QvAlT6_CQCWQoCqtsbnXQ6bi5Y6jpeALLDbse1JKmG2caouzizqpqkIyFc3ZqhqoJOMmBoC3osOay0qAWM0lGvv1u7TZU7-M","dq":"C8uwnfB40Gts284OvYc_6W9whfxKaHoW1eFewkW8hi2cmRm05VFiBitonlIkE5IcbeKbJcixdTphkcthRYp_-K7ZJov-jmu9fFeQQ7eDYfgCtWKTcV5876EqrDJ7LvhD8sL4FamqAKf-hq_qtjfWKzPVobA8-q2pfvVvrULrdac","qi":"mAXOTpZF54XbnUQj3vVy5oFh2HtVyXZuCuTvDELKt6Z4x74xUBU7KCm_mq-tYEb_XWy_3trkQ-stP4RRAGwqLmFprxCX-G2uJOCBK6vpVsfDPUhSDe3CVEfyWVWu2knritBBhJX4dG-8I_cjFgCBFNz46Y9WG_5CdqkmshlpVDI"}


The first JSON document needs to be provided to any clients wishing to use OpenID Connect when interacting with Stonecutter, while the second should be stored in a file --- for example "rsa-keypair.json" --- which should be kept secure.

To deploy Stonecutter with this key, the rsa-keypair.json file needs to be placed in a directory accessible by the deployed instance, and the Stonecutter instance should be started with the environment variable ```RSA_KEYPAIR_FILE_PATH``` pointing to its location.

If deploying using the Snap CI tool (https://snap-ci.com/), this process is automated by including the rsa-keypair.json file in the secure files for the snap stage responsible for deployment, with the filename "rsa-keypair_<SNAP_STAGE_NAME>.json".  The ```ops/deploy_snap.sh``` script will then manage copying the file to the appropriate location on the target, and starting the app with the environment variable set.
