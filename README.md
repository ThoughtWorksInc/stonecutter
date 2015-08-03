# stonecutter

[![Build Status](https://snap-ci.com/ThoughtWorksInc/stonecutter/branch/master/build_image)](https://snap-ci.com/ThoughtWorksInc/stonecutter/branch/master)

A D-CENT project: an easily deployable oauth server for small organisations.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Development VM

You can also develop and run the application in a VM.  You will need [Vagrant][] installed.

navigate to the ops/ directory of the project and run:

    vagrant up

When the VM has started, access the virtual machine by running:

    vagrant ssh

The source folder will be located at `/var/stonecutter`

After initial setup you will need to run:

    cd /var/stonecutter
    npm install

This will take a while (upwards of 10 minutes).

[Vagrant]: https://www.vagrantup.com

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

####Simply type
```
gulp server
```

### Running test suite

#### To run all test, run this in your virtual machine
```
lein midje
```

### Customising the theme

####Colours

Currently without getting your hands dirty we offer customisation of the header, logo and navigation tabs.

Locate /assets/stylesheets/application.scss and modify the line that looks like this:
```
//@import "theme"; // uncomment to apply your theme
```

To look like this:
```
@import "theme"; // uncomment to apply your theme
```

Then update the variable values inside /assets/stylesheets/theme.scss 

####Logo

You can either update the /assets/images/logo.svg or supply your own.  If you choose you own be sure to enable the theme and update the path in assets/stylesheets/theme.scss.

####Fav icon

You can update the favicons located in /assets/icons/.