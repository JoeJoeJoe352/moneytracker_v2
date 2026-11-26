import { computed, DOCUMENT, effect, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withProps,
    withState,
} from '@ngrx/signals';
import { UserDataStore } from '@app/shared/stores/user-data-store';
import { filter, map } from 'rxjs';

interface LayoutState {
    /**
     * Nyitva van-e a mobilmenü (privát)
     */
    _isMobileMenuOpen: boolean;
}

const initialState: LayoutState = {
    _isMobileMenuOpen: false,
};

/**
 * Az alkalmazás keretének (oldalsáv, mobilmenü) állapotát tároló store
 */
export const LayoutStore = signalStore(
    { providedIn: 'root' },
    withState(initialState),
    withProps(() => ({
        /**
         * Aktuális URL útvonala (query és fragment nélkül) (privát)
         */
        _currentPath: toSignal(
            inject(Router).events.pipe(
                filter((event) => event instanceof NavigationEnd),
                map((event) => event.urlAfterRedirects.split(/[?#]/)[0]),
            ),
            { initialValue: '' },
        ),
    })),
    withComputed(({ _isMobileMenuOpen, _currentPath }, userData = inject(UserDataStore)) => ({
        /**
         * Nyitva van-e a mobilmenü
         */
        isMobileMenuOpen: computed(() => _isMobileMenuOpen()),
        /**
         * Látható-e az oldalsáv. Kijelentkezett user a welcome (landing) oldalon nem látja
         */
        isSidebarVisible: computed(
            () => !(_currentPath() === '/welcome' && !userData.isUserLogged()),
        ),
    })),
    withMethods((store) => ({
        /**
         * Mobilmenü lenyitása/bezárása
         */
        toggleMobileMenu(): void {
            patchState(store, (state) => ({ _isMobileMenuOpen: !state._isMobileMenuOpen }));
        },

        /**
         * Mobilmenü bezárása
         */
        closeMobileMenu(): void {
            patchState(store, { _isMobileMenuOpen: false });
        },
    })),
    withHooks({
        onInit(store, router = inject(Router), document = inject(DOCUMENT)) {
            // Nyitott mobil menü alatt letiltjuk az oldal görgetését
            effect(() => {
                document.documentElement.classList.toggle(
                    'mobile-menu-scroll-lock',
                    store.isMobileMenuOpen(),
                );
            });

            // Navigáció után bezárjuk a mobilmenüt
            router.events
                .pipe(
                    filter((event) => event instanceof NavigationEnd),
                    takeUntilDestroyed(),
                )
                .subscribe(() => store.closeMobileMenu());
        },
    }),
);
