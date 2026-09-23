import { Routes } from '@angular/router';
import { authGuard } from './auth-guard';

/**
 * Routes lazy load-al van betöltve, hogy ne lépjük túl a bundle-size budget-et
 */
export const routes: Routes = [
    {
        path: '',
        loadComponent: () =>
            import('./features/main-page/main-page-component').then((m) => m.MainPageComponent),
        canActivate: [authGuard],
    },
    {
        path: 'welcome',
        loadComponent: () =>
            import('./features/welcome/welcome-component').then((m) => m.WelcomeComponent),
    },
    {
        path: 'transactions',
        loadComponent: () =>
            import('./features/transaction/transactions-page-component').then(
                (m) => m.TransactionsPageComponent,
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
        loadComponent: () =>
            import('./features/error/error-page-component').then((m) => m.ErrorPageComponent),
    },
];
