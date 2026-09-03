import { inject, Pipe, PipeTransform } from '@angular/core';
import { CurrencyCodesEnum } from '../enums';
import { WalletDataUtil } from '../../features/wallet/wallet-data-util';

@Pipe({
    name: 'currencySymbol',
    standalone: true,
})
export class CurrencySymbolPipe implements PipeTransform {
    private walletUtils = inject(WalletDataUtil);

    public transform(currencyCode: CurrencyCodesEnum): string {
        return this.walletUtils.getCurrencySymbolForCurrencyCode(currencyCode);
    }
}
