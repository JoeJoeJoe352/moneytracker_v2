import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { DOCUMENT, signal, WritableSignal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Event, NavigationEnd, NavigationStart, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { UserDataStore } from '@app/shared/stores/user-data-store';
import { LayoutStore } from './layout-store';

describe('LayoutStore (Vitest)', () => {
    let store: InstanceType<typeof LayoutStore>;
    let routerEvents: Subject<Event>;
    let isUserLogged: WritableSignal<boolean>;
    let document: Document;

    beforeEach(() => {
        routerEvents = new Subject();
        isUserLogged = signal(false);

        TestBed.configureTestingModule({
            providers: [
                { provide: Router, useValue: { events: routerEvents } },
                { provide: UserDataStore, useValue: { isUserLogged } },
            ],
        });

        store = TestBed.inject(LayoutStore);
        document = TestBed.inject(DOCUMENT);
    });

    afterEach(() => {
        document.documentElement.classList.remove('mobile-menu-scroll-lock');
    });

    function navigateTo(url: string): void {
        routerEvents.next(new NavigationEnd(1, url, url));
    }

    it('should start with the mobile menu closed', () => {
        expect(store.isMobileMenuOpen()).toBe(false);
    });

    it('should open and close the mobile menu on toggle', () => {
        store.toggleMobileMenu();
        expect(store.isMobileMenuOpen()).toBe(true);

        store.toggleMobileMenu();
        expect(store.isMobileMenuOpen()).toBe(false);
    });

    it('should close the mobile menu', () => {
        store.toggleMobileMenu();

        store.closeMobileMenu();

        expect(store.isMobileMenuOpen()).toBe(false);
    });

    it('should keep the mobile menu closed when closing an already closed menu', () => {
        store.closeMobileMenu();

        expect(store.isMobileMenuOpen()).toBe(false);
    });

    it('should close the mobile menu after navigation ends', () => {
        store.toggleMobileMenu();

        navigateTo('/wallets');

        expect(store.isMobileMenuOpen()).toBe(false);
    });

    it('should keep the mobile menu open on other router events', () => {
        store.toggleMobileMenu();

        routerEvents.next(new NavigationStart(1, '/wallets'));

        expect(store.isMobileMenuOpen()).toBe(true);
    });

    it('should hide the sidebar on the welcome page for a logged out user', () => {
        navigateTo('/welcome');

        expect(store.isSidebarVisible()).toBe(false);
    });

    it('should hide the sidebar on the welcome page ignoring query and fragment', () => {
        navigateTo('/welcome?ref=mail#top');

        expect(store.isSidebarVisible()).toBe(false);
    });

    it('should show the sidebar on the welcome page for a logged in user', () => {
        isUserLogged.set(true);

        navigateTo('/welcome');

        expect(store.isSidebarVisible()).toBe(true);
    });

    it('should show the sidebar on other pages for a logged out user', () => {
        navigateTo('/wallets');

        expect(store.isSidebarVisible()).toBe(true);
    });

    it('should show the sidebar again after the user logs in on the welcome page', () => {
        navigateTo('/welcome');

        isUserLogged.set(true);

        expect(store.isSidebarVisible()).toBe(true);
    });

    it('should show the sidebar again after leaving the welcome page', () => {
        navigateTo('/welcome');

        navigateTo('/transactions');

        expect(store.isSidebarVisible()).toBe(true);
    });

    it('should lock page scrolling while the mobile menu is open', () => {
        store.toggleMobileMenu();
        TestBed.tick();

        expect(document.documentElement.classList.contains('mobile-menu-scroll-lock')).toBe(true);
    });

    it('should unlock page scrolling when the mobile menu closes', () => {
        store.toggleMobileMenu();
        TestBed.tick();

        store.closeMobileMenu();
        TestBed.tick();

        expect(document.documentElement.classList.contains('mobile-menu-scroll-lock')).toBe(false);
    });
});
