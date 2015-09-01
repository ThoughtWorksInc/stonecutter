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
    npm install

This will take a while (upwards of 10 minutes).

[Vagrant]: https://www.vagrantup.com
[Ansible]: http://docs.ansible.com/ansible/intro_installation.html

## Running
Before starting the server, build the views by running:

    gulp build

To start a web server for the application in development mode, run:

    lein ring server-headless

NB: running the application like this will save users into an in memory cache that will be destroyed as soon as the app is shutdown.

To start a web server with users persisted to mongodb, ensure you have mongo running locally and run:

    lein run

## Running the static frontend

### Getting started

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

### Running the prototype

Simply type:

```
gulp server
```

## Running test suite

To run all tests, run this in your virtual machine

```
lein midje
```

## Customising the app

Optional environment variables can be set up to customise app name, colour scheme and logo.

###App name

App name is used anywhere where the application refers to itself, e.g. "Register with {App name}".
To set the app name:

* Set the environment variable `APP_NAME`

The content of any HTML elements with the class `.clj--app-name` will be replaced with the app name.

### Logo

Logo is used in the header. Maximum dimensions (W x H) 110px x 50px.
To set the logo:

* Set the environment variable `STATIC_RESOURCES_DIR_PATH` to a directory containing the logo.
* **NOTE: Anything inside this directory will be served as a static resource, including subdirectories.**
* Set the environment variable `LOGO_FILE_NAME` to the logo file name including the extension, e.g. logo.png

### Favicon

Favicon should be an .ico file.
To set the favicon:

* Set the environment variable `STATIC_RESOURCES_DIR_PATH` to a directory containing the favicon
if you haven't already done so for the logo.
* **NOTE: Anything inside this directory will be served as a static resource, including subdirectories.**
* Set the environment variable `FAVICON_FILE_NAME` to the favicon file name, e.g. my-favicon.ico

### Colours

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

### Adding public-private keypair for OpenID Connect

To generate a public-private keypair in Json Web-key (JWK) format, enter the following at the command line:

```
> lein gen-keypair <key-id>
```

where the key-id is a custom identifier for the key (for example "20150824-stonecutter").

This will generate output similar to the following:
```
JWK public key for client:
==========================
{"kty":"RSA","kid":"20150824-stonecutter","n":"m38hDunIOBX4DdalnuoNoT7nVdn5gXprVFUlBX3KbDIwyKznX2QZQLDn_4_b94UsYlh1Vf33pO9TO9tsj2Hf1WdFQO72WqFUxFOk3ITc7OTc7p5oZhWYXsKCJh5dLl9G4tOxZ_vD-frD7c0M_-IUWQ9cuk7XulDNJqzHKSEHvbugokw-vOb9fI2CtBU9HtWHkbe3e8cJdbEN4zD7Qw7BrG5zCENGuWIMpe9XIpZTM0jwiclxNacNhU_eOiRk9wg7hHovGqFuSU8x0oohtaNe91YUCJsfnmHQZTARc8tGJOwhx4A8VAUnqVmmm7GCGx0CqvbzRtFolTbn39m3jMTtoQ","e":"AQAB"}


JWK including private key for stonecutter:
==========================================
{"kty":"RSA","kid":"20150824-stonecutter","n":"m38hDunIOBX4DdalnuoNoT7nVdn5gXprVFUlBX3KbDIwyKznX2QZQLDn_4_b94UsYlh1Vf33pO9TO9tsj2Hf1WdFQO72WqFUxFOk3ITc7OTc7p5oZhWYXsKCJh5dLl9G4tOxZ_vD-frD7c0M_-IUWQ9cuk7XulDNJqzHKSEHvbugokw-vOb9fI2CtBU9HtWHkbe3e8cJdbEN4zD7Qw7BrG5zCENGuWIMpe9XIpZTM0jwiclxNacNhU_eOiRk9wg7hHovGqFuSU8x0oohtaNe91YUCJsfnmHQZTARc8tGJOwhx4A8VAUnqVmmm7GCGx0CqvbzRtFolTbn39m3jMTtoQ","e":"AQAB","d":"kRSlaH-xorrErUy3TLU-MFM7jnuI80igOZgTqbL7GcYehC3m1rbTZOtqGqVD7AaiKcQ0_h2uYII3m6KYAJOmPztSf0o2KstaBq-wI1wHsTO7-xtrdsvxVYCP5DbyY-Dbh6lSXh2mdWeGRSrLVTfAGnRd5SrI1vqq3snYLMS3r0qSubpVjo1yGjcOitxgJWgvdRq2FRPplgRlnoaiMd5jVCNXvSP-2XXeIQq0nz_GLcqcjOI0hqPsEPFcdjtL9PdwXa7v3cmrjOcWprlFzBQVTL6YvT_kCKIghJsG9ksJoUzTafHUAYUBdfgQSTi0q-kommHr3SyQhL1aN4Khqm3jLQ","p":"9wOYB-B7mhbGsxh7qago75DqUhp3L2x56yP1pYA2dV0TBNQz2jlGjAJ-xzMCQ-AMOpGNtzWJ28A-aDcUo1ZXIam3qktCha38fIAuvgKR7k0tnjhLawIONBaA-OlSorszlAWdHJ3_4ckn0c_u9Zne0SHkQESJNY7ES23-Sca3AL8","q":"oSc_HO3y61wgMUDDTMtMFYaJA9UdO4fIEfEyu46VvgvIN2kvf2ayHTb01Pk-XsoL2OJUcmjg4g19sBt8xGCRU8as4DOBHb22rbYQ7qTa4ewTtqLQBTnrTMzWZLYN2JYCZydFCW63z9zypC34Uoi_AF-teDprNY-eepRkr9JbSZ8","dp":"KDhGlenAVmuk-N5grFQ8Lh3LeYjjpS4lf9sAEW2Z8GwyP5QJyVuQGBYD7I1qrgCaHSM8DvvBsa1QvAlT6_CQCWQoCqtsbnXQ6bi5Y6jpeALLDbse1JKmG2caouzizqpqkIyFc3ZqhqoJOMmBoC3osOay0qAWM0lGvv1u7TZU7-M","dq":"C8uwnfB40Gts284OvYc_6W9whfxKaHoW1eFewkW8hi2cmRm05VFiBitonlIkE5IcbeKbJcixdTphkcthRYp_-K7ZJov-jmu9fFeQQ7eDYfgCtWKTcV5876EqrDJ7LvhD8sL4FamqAKf-hq_qtjfWKzPVobA8-q2pfvVvrULrdac","qi":"mAXOTpZF54XbnUQj3vVy5oFh2HtVyXZuCuTvDELKt6Z4x74xUBU7KCm_mq-tYEb_XWy_3trkQ-stP4RRAGwqLmFprxCX-G2uJOCBK6vpVsfDPUhSDe3CVEfyWVWu2knritBBhJX4dG-8I_cjFgCBFNz46Y9WG_5CdqkmshlpVDI"}
```

The first JSON document needs to be provided to any clients wishing to use OpenID Connect when interacting with Stonecutter, while the second should be stored in a file --- for example "rsa-keypair.json" --- which should be kept secure.

To deploy Stonecutter with this key, the rsa-keypair.json file needs to be placed in a directory accessible by the deployed instance, and the Stonecutter instance should be started with the environment variable ```RSA_KEYPAIR_FILE_PATH``` pointing to its location.

If deploying using the Snap CI tool (https://snap-ci.com/), this process is automated by including the rsa-keypair.json file in the secure files for the snap stage responsible for deployment, with the filename "rsa-keypair_<SNAP_STAGE_NAME>.json".  The ```ops/deploy_snap.sh``` script will then manage copying the file to the appropriate location on the target, and starting the app with the environment variable set.
