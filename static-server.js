var express = require('express');
var app = express();

app.set('port', (process.env.PORT || 7069));

var pageData = {
  "devMode": true,
  "javascriptsBase": "/javascripts",
  "stylesheetsBase": "/stylesheets",
  "imagesBase": "/images",
  "applicationName": "Stonecutter",
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

app.get('/sign-in', function (req, res) {
  customRender(res, 'sign-in', pageData);
});

app.get('/register', function (req, res) {
  customRender(res, 'register', pageData);
});

app.all('/authorise', function (req, res) {
  customRender(res, 'authorise', pageData);
});

app.get('/forgot-password', function (req, res) {
  customRender(res, 'forgot-password', pageData);
});

app.get('/profile', function (req, res) {
  customRender(res, 'profile', pageData);
});


// temp stuff for demo

app.get('/greenparty/register', function (req, res) {
  customRender(res, 'greenparty/register', pageData);
});
app.get('/greenparty/sign-in', function (req, res) {
  customRender(res, 'greenparty/sign-in', pageData);
});
app.all('/greenparty/authorise', function (req, res) {
  customRender(res, 'greenparty/authorise', pageData);
});


app.listen(app.get('port'), function () {
  console.log('Node app is running on port', app.get('port'));
});