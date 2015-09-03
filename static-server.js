var express = require('express');
var app = express();

app.set('port', (process.env.PORT || 7069));

var pageData = {
  "devMode": true,
  "javascriptsBase": "/javascripts",
  "stylesheetsBase": "/stylesheets",
  "imagesBase": "/images",
  "applicationName": "Stonecutter",
  "serviceName": "Objective8",
  "demoAppURL": "http://localhost:7778"
};

app.use('/', express.static(__dirname + '/resources/public'));

app.set('view engine', 'jade');
app.set('views', './assets/jade');

function beforeAllFilter(req, res, next) {
  app.locals.pretty = true;
  next();
}

app.all('*', beforeAllFilter);

function customRender(res, template, data) {
  res.render(template, data, function (err, html) {
    var cleanHTML = html.replace(/>!/g, '>');
    res.send(cleanHTML);
  });
}

app.get('/', function (req, res) {
  customRender(res, 'index', pageData);
});

app.get('/library', function (req, res) {
  customRender(res, 'library', pageData);
});

app.all('/routes', function (req, res) {
  customRender(res, 'routes', pageData);
});


app.all('/change-password', function (req, res) {
  customRender(res, 'change-password', pageData);
});

app.all('/confirmation-sign-in', function (req, res) {
  customRender(res, 'confirmation-sign-in', pageData);
});

app.all('/profile-created', function (req, res) {
  customRender(res, 'profile-created', pageData);
});

app.all('/profile-deleted', function (req, res) {
  customRender(res, 'profile-deleted', pageData);
});

app.all('/authorise', function (req, res) {
  customRender(res, 'authorise', pageData);
});

app.all('/authorise-failure', function (req, res) {
  customRender(res, 'authorise-failure', pageData);
});

app.get('/forgot-password', function (req, res) {
  customRender(res, 'forgot-password', pageData);
});

app.get('/forgot-password-confirmation', function (req, res) {
  customRender(res, 'forgot-password-confirmation', pageData);
});

app.get('/reset-password', function(req, res) {
  customRender(res, 'reset-password', pageData)
});

app.all('/profile', function (req, res) {
  customRender(res, 'profile', pageData);
});

app.all('/unshare-profile-card', function (req, res) {
  customRender(res, 'unshare-profile-card', pageData);
});

app.all('/confirm-email-expired', function (req, res) {
  customRender(res, 'confirm-email-expired', pageData);
});

app.all('/confirm-email-resent', function (req, res) {
  customRender(res, 'confirm-email-resent', pageData);
});

app.get('/delete-account', function (req, res) {
  customRender(res, 'delete-account', pageData);
});

app.get('/error-404', function (req, res) {
  customRender(res, 'error-404', pageData);
});

app.get('/error-500', function (req, res) {
  customRender(res, 'error-500', pageData);
});

app.get('/sign-out', function (req, res) {
  res.redirect('/');
});

app.get('/admin/sign-in', function (req, res) {
  customRender(res, 'admin-sign-in', pageData);
});
app.all('/admin/user-list', function (req, res) {
  customRender(res, 'user-list', pageData);
});


app.listen(app.get('port'), function () {
  console.log('Node app is running on port', app.get('port'));
});