# stonecutter

[![Build Status](https://snap-ci.com/d-cent/stonecutter/branch/master/build_image)](https://snap-ci.com/d-cent/stonecutter/branch/master)

A D-CENT project: an easily deployable oauth server for small organisations.

## Development VM

You can develop and run the application in a VM to ensure that the correct versions of Stonecutter's dependencies
are installed. You will need [VirtualBox][], [Vagrant][] and [Ansible][] installed.

First, clone the repository.

Navigate to the ops/ directory of the project and run:

    vagrant up development

The first time this is run, it will provision and configure a new VM.

When the VM has started, access the virtual machine by running:

    vagrant ssh

The source folder will be located at `/var/stonecutter`.

After initial setup, navigate to the source directory with:

    cd /var/stonecutter

[Vagrant]: https://www.vagrantup.com
[Ansible]: http://docs.ansible.com/ansible/intro_installation.html
[VirtualBox]: https://www.virtualbox.org/

### Running

To start the app, run:

    ./start_app_vm.sh

To start a web server for the application in development mode, run:

    lein ring server-headless

NB: running the application like this will save users into an in memory cache that will be destroyed as soon as the app is shutdown.

### Running test suite

To run all tests, use this command:

```
lein test
```

Commands and aliases can be found in the project.clj file. 

### Running the prototype

Simply type:

```
gulp server
```

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

## Architecture

The Continuous Delivery build and deployment architecture is documented [here] (https://docs.google.com/a/thoughtworks.com/drawings/d/1FZ35v27_pBym_NqzLbqVP_TwnHVBHNwFQss_Lzbs1bU/edit?usp=sharing).

The Hosting Architecture is documented [here] (https://docs.google.com/a/thoughtworks.com/drawings/d/1mgdxe0Q0uYZloZLFLvlwyKRx4iUangEAn4aV2qm-zWs/edit?usp=sharing).


## Deployment

To deploy using Docker, see [here](docs/DOCKER.md).