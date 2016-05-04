# Configuration

The following environment variables can be passed to the application.

## Required:

- **RSA_KEYPAIR_FILE_PATH** - location of json file containing RSA keypair for Open ID Connect.
 
## Optional:

- **HOST** - defaults to localhost
- **PORT** - defaults to 5000
- **BASE_URL** - your application URI or IP address. Defaults to localhost:5000
- **CLIENT_CREDENTIALS_FILE_PATH** - location of the yml fle containing the clients your instance of Stonecutter will communicate with. See *config/clients.yml* for an example. Defaults to client-credentials.yml
- **APP_NAME** - shown on the index page. Defaults to "Stonecutter"
- **PASSWORD_RESET_EXPIRY** - time in hours before reset password e-mail expires. Defaults to 24.
- **OPEN_ID_CONNECT_ID_TOKEN_LIFETIME_MINUTES** - time in minutes before OpenID Connect token expires. Defaults to 10.
- **INVITE_EXPIRY** - time in days before invite e-mail expires. Defaults to 7.
- **MONGO_URI** - URI for the MongoDB database. Set automatically when using Docker. Defaults to "mongodb://localhost:27017/stonecutter"
- **MONGO_DB_NAME** - name of the MongoDB database. Defaults to stonecutter.

### Customisations for the header

- **HEADER_BG_COLOR** - defaults to #eee
- **HEADER_FONT_COLOR** - defaults to #222.
- **HEADER_FONT_COLOR_HOVER** - defaults to #00d3ca.
- **STATIC_RESOURCES_DIR_PATH** - the directory containing the custom logo and favicon
- **LOGO_FILE_NAME** - defaults to the Stonecutter logo
- **FAVICON_FILE_NAME** - defaults to the Stonecutter icon

### Details used when setting up an admin user

- **ADMIN_FIRST_NAME** - defaults to "Mighty"
- **ADMIN_LAST_NAME** - defaults to "Admin"
- **ADMIN_LOGIN** - required for the creation of an admin user
- **ADMIN_PASSWORD** - required for the creation of an admin user

### Email configuration

- **EMAIL_SCRIPT_PATH** - location of script used to send emails.
- **EMAIL_SERVICE_PROVIDER** - used when deploying using ansible to select the email script. See [here](../README.md#adding-an-email-provider).
