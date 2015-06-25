var express = require('express');
var app = express();

app.set('port', (process.env.PORT || 7069));

var pageData = {
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

app.get('/', function(req, res){
  res.render('index', pageData);
});

app.get('/library', function(req, res){
  res.render('library', pageData);
});

app.get('/sign-in', function(req, res){
  res.render('sign-in', pageData);
});

app.get('/register', function(req, res){
  res.render('register', pageData);
});

app.all('/authorise', function(req, res){
  res.render('authorise', pageData);
});

app.get('/forgot-password', function(req, res){
  res.render('forgot-password', pageData);
});

app.get('/profile', function(req, res){
  res.render('profile', pageData);
});


// temp stuff for demo

app.get('/greenparty/register', function(req, res){
  res.render('greenparty/register', pageData);
});
app.get('/greenparty/sign-in', function(req, res){
  res.render('greenparty/sign-in', pageData);
});
app.all('/greenparty/authorise', function(req, res){
  res.render('greenparty/authorise', pageData);
});





app.listen(app.get('port'), function() {
  console.log('Node app is running on port', app.get('port'));
});