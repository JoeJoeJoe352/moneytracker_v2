import { Routes } from '@angular/router';
import { authGuard } from './auth-guard-guard';

/**
 * Routes lazy load-al van betöltve, hogy ne lépjük túl a bundle-size budget-et
 */
export const routes: Routes = [
    {
        path: '',
        loadComponent: () => import('./features/main-page/main-page-component').then((m) => m.MainPage),
        canActivate: [authGuard],
    },
    {
        path: 'welcome',
        loadComponent: () => import('./features/welcome/welcome').then((m) => m.Welcome),
    },
    {
        path: 'transactions',
        loadComponent: () =>
            import('./features/history-page/transactions-page-component').then(
                (m) => m.TransactionsPage,
            ),
        canActivate: [authGuard],
    },
    {
        path: 'wallets',
        loadComponent: () =>
            import('./features/wallet/wallets-page-component').then(
                (m) => m.WalletsPageComponent,
            ),
        canActivate: [authGuard],
    },
    {
        path: '**',
        loadComponent: () => import('./features/error/error-page-component').then((m) => m.ErrorPage),
    },
];
