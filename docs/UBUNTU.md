# Deployment to ubuntu server (e.g. through digital ocean)

### Provision
- Provision an ubuntu server machine (can be a cloud server such as digital ocean)
- A machine with 1gb RAM and 30gb hard disk has been sufficient for early tests
- Enable connection to the box via ssh - [how to](https://www.digitalocean.com/community/tutorials/how-to-set-up-ssh-keys--2) 

### Generate a public-private keypair in Json Web-key (JWK) format for OpenID Connect

- Run the command `lein gen-keypair <key-id>` where the key-id is a custom identifier for the key (for example "20150824-stonecutter").

This will generate output similar to the following:

    JWK public key for client:
    ==========================
    {"kty":"RSA","kid":"20150824-stonecutter","n":"m38hDunIOBX4DdalnuoNoT7nVdn5gXprVFUlBX3KbDIwyKznX2QZQLDn_4_b94UsYlh1Vf33pO9TO9tsj2Hf1WdFQO72WqFUxFOk3ITc7OTc7p5oZhWYXsKCJh5dLl9G4tOxZ_vD-frD7c0M_-IUWQ9cuk7XulDNJqzHKSEHvbugokw-vOb9fI2CtBU9HtWHkbe3e8cJdbEN4zD7Qw7BrG5zCENGuWIMpe9XIpZTM0jwiclxNacNhU_eOiRk9wg7hHovGqFuSU8x0oohtaNe91YUCJsfnmHQZTARc8tGJOwhx4A8VAUnqVmmm7GCGx0CqvbzRtFolTbn39m3jMTtoQ","e":"AQAB"}


    JWK including private key for stonecutter:
    ==========================================
    {"kty":"RSA","kid":"20150824-stonecutter","n":"m38hDunIOBX4DdalnuoNoT7nVdn5gXprVFUlBX3KbDIwyKznX2QZQLDn_4_b94UsYlh1Vf33pO9TO9tsj2Hf1WdFQO72WqFUxFOk3ITc7OTc7p5oZhWYXsKCJh5dLl9G4tOxZ_vD-frD7c0M_-IUWQ9cuk7XulDNJqzHKSEHvbugokw-vOb9fI2CtBU9HtWHkbe3e8cJdbEN4zD7Qw7BrG5zCENGuWIMpe9XIpZTM0jwiclxNacNhU_eOiRk9wg7hHovGqFuSU8x0oohtaNe91YUCJsfnmHQZTARc8tGJOwhx4A8VAUnqVmmm7GCGx0CqvbzRtFolTbn39m3jMTtoQ","e":"AQAB","d":"kRSlaH-xorrErUy3TLU-MFM7jnuI80igOZgTqbL7GcYehC3m1rbTZOtqGqVD7AaiKcQ0_h2uYII3m6KYAJOmPztSf0o2KstaBq-wI1wHsTO7-xtrdsvxVYCP5DbyY-Dbh6lSXh2mdWeGRSrLVTfAGnRd5SrI1vqq3snYLMS3r0qSubpVjo1yGjcOitxgJWgvdRq2FRPplgRlnoaiMd5jVCNXvSP-2XXeIQq0nz_GLcqcjOI0hqPsEPFcdjtL9PdwXa7v3cmrjOcWprlFzBQVTL6YvT_kCKIghJsG9ksJoUzTafHUAYUBdfgQSTi0q-kommHr3SyQhL1aN4Khqm3jLQ","p":"9wOYB-B7mhbGsxh7qago75DqUhp3L2x56yP1pYA2dV0TBNQz2jlGjAJ-xzMCQ-AMOpGNtzWJ28A-aDcUo1ZXIam3qktCha38fIAuvgKR7k0tnjhLawIONBaA-OlSorszlAWdHJ3_4ckn0c_u9Zne0SHkQESJNY7ES23-Sca3AL8","q":"oSc_HO3y61wgMUDDTMtMFYaJA9UdO4fIEfEyu46VvgvIN2kvf2ayHTb01Pk-XsoL2OJUcmjg4g19sBt8xGCRU8as4DOBHb22rbYQ7qTa4ewTtqLQBTnrTMzWZLYN2JYCZydFCW63z9zypC34Uoi_AF-teDprNY-eepRkr9JbSZ8","dp":"KDhGlenAVmuk-N5grFQ8Lh3LeYjjpS4lf9sAEW2Z8GwyP5QJyVuQGBYD7I1qrgCaHSM8DvvBsa1QvAlT6_CQCWQoCqtsbnXQ6bi5Y6jpeALLDbse1JKmG2caouzizqpqkIyFc3ZqhqoJOMmBoC3osOay0qAWM0lGvv1u7TZU7-M","dq":"C8uwnfB40Gts284OvYc_6W9whfxKaHoW1eFewkW8hi2cmRm05VFiBitonlIkE5IcbeKbJcixdTphkcthRYp_-K7ZJov-jmu9fFeQQ7eDYfgCtWKTcV5876EqrDJ7LvhD8sL4FamqAKf-hq_qtjfWKzPVobA8-q2pfvVvrULrdac","qi":"mAXOTpZF54XbnUQj3vVy5oFh2HtVyXZuCuTvDELKt6Z4x74xUBU7KCm_mq-tYEb_XWy_3trkQ-stP4RRAGwqLmFprxCX-G2uJOCBK6vpVsfDPUhSDe3CVEfyWVWu2knritBBhJX4dG-8I_cjFgCBFNz46Y9WG_5CdqkmshlpVDI"}


- The public key needs to be provided to any clients wishing to use OpenID Connect when interacting with Stonecutter
- The private key should be stored in a file and kept secure.

### Configure with ansible
- Install Ansible
- In file *ops/dob.inventory* replace the IP address with the IP address of your ubuntu server machine
- Copy the *config/clients.yml* file and add the details of the clients you want to use Stonecutter with
- *Optional* - Copy a custom logo and favicon into the *config/files/* directory
- Use the *stonecutter_ansible.env* found in the */config* directory and replace the empty strings with your credentials and save it for use in the next step. Take note of the file path. You can find more information about the configuration variables [here](./CONFIG.md).
- Create a *ops/roles/nginx/files/secure/* directory, and copy your SSL certificate and key files there, with the names *stonecutter.key* and *stonecutter.crt*.

Run Ansible playbook:

  The following command will install necessary packages and configure them (it will take a few minutes).
  ```
  ansible-playbook ops/dob_playbook.yml -i ops/dob.inventory --extra-vars "CONFIG_FILE_PATH={config file path from the previous step without the curly braces}"
  ```
  
### Deploy application to the server

  The following will copy the application to the server and start it running as a service in a docker container.
  Once complete you should be able to access the app at your IP address.

  ```
  chmod +x deploy_prod.sh
  REMOTE_USER={username on server} SERVER_IP={IP address of server} ./deploy_prod.sh
  ```
