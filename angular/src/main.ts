import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import 'zone.js'
// @ts-ignore: csak CSS-t szeretném behúzni és annak nincs module-ja, ezért a typescript hibát dobna
import 'bootstrap/dist/css/bootstrap.min.css';

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
