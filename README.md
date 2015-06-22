# stonecutter

A D-CENT project: an easily deployable oauth server for small organisations.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Development VM

You can also develop and run the application in a VM.  You will need [Vagrant][] installed.

navigate to the ops/ directory of the project and run:

    vagrant up

When the VM has started, the source folder will be located at /var/stonecutter


[Vagrant]: https://www.vagrantup.com

## Running

To start a web server for the application in development mode, run:

    lein ring server-headless

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
