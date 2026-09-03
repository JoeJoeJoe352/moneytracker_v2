import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { WalletsListComponent } from './wallets-list-component';
import { WalletDataInterface } from './interfaces';
import { CurrencyCodesEnum, WalletTypesEnum } from '../../shared/enums';

describe('WalletsListComponent (Vitest)', () => {
    let fixture: ComponentFixture<WalletsListComponent>;
    let component: WalletsListComponent;

    const wallets: WalletDataInterface[] = [
        { id: 1, name: 'Napi költés', currencyCode: CurrencyCodesEnum.huf, type: WalletTypesEnum.default, sum: 0 },
        { id: 2, name: 'Megtakarítás', currencyCode: CurrencyCodesEnum.eur, type: WalletTypesEnum.savings, sum: 0 },
    ];

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [WalletsListComponent],
            providers: [provideTranslateService()],
        }).compileComponents();

        fixture = TestBed.createComponent(WalletsListComponent);
        component = fixture.componentInstance;
        component.walletListData = wallets;
    });

    it('should render exactly one card per wallet', () => {
        fixture.detectChanges();

        const cards = fixture.nativeElement.querySelectorAll('app-wallet-card-component');
        expect(cards.length).toBe(2);
    });

    it('should emit walletCardClicked with the wallet that was clicked, not just the first one', () => {
        fixture.detectChanges();

        let clicked: WalletDataInterface | undefined;
        component.walletCardClicked.subscribe((wallet) => (clicked = wallet));

        const cards = fixture.nativeElement.querySelectorAll('app-wallet-card-component');
        cards[1].querySelector('.wallet-card').click();

        expect(clicked).toEqual(wallets[1]);
    });
});
