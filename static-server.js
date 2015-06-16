var express = require('express');
var app = express();

app.set('port', (process.env.PORT || 7070));

var pageData = {
  "javascriptsBase": "/assets/javascripts",
  "stylesheetsBase": "/assets/stylesheets",
  "imagesBase": "/assets/images"
};

app.use('/assets', express.static(__dirname + '/resources/public'));

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

app.get('/authorise', function(req, res){
  res.render('authorise', pageData);
});

app.get('/profile', function(req, res){
  res.render('profile', pageData);
});

app.listen(app.get('port'), function() {
  console.log('Node app is running on port', app.get('port'));
});