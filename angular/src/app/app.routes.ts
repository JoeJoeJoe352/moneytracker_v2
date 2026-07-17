import { Routes } from '@angular/router';
import { Welcome } from './features/welcome/welcome';
import { MainPage } from './features/main-page/main-page-component';
import { ErrorPage } from './features/error/error-page-component';
import { authGuard } from './auth-guard-guard';
import { TransactionsPage } from './features/history-page/transactions-page-component';

export const routes: Routes = [
    {path: '', component: MainPage, canActivate: [authGuard]},
    {path: 'welcome', component: Welcome},
    {path: 'transactions', component: TransactionsPage, canActivate: [authGuard]},
    {path: '**', component: ErrorPage}
];
