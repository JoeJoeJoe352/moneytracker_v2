import { Routes } from '@angular/router';
import { Welcome } from './features/welcome/welcome';
import { MainPage } from './features/main-page/main-page-component';
import { ErrorPage } from './features/error/error-page-component';
import { authGuardGuard } from './auth-guard-guard';

export const routes: Routes = [
    {path: '', component: MainPage, canActivate: [authGuardGuard]},
    {path: 'welcome', component: Welcome},
    {path: '**', component: ErrorPage}
];
