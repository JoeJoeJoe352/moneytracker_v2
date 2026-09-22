import {
    Component,
    computed,
    ElementRef,
    inject,
    input,
    Signal,
} from '@angular/core';
import { TransactionListElementData } from './interfaces';
import { CurrencyFormatPipe } from '../../shared/pipes/currency-format-pipe';
import { MatCard } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { TransactionTypeEnum } from '../../shared/enums';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'app-transaction-card',
    templateUrl: './transaction-card-component.html',
    styleUrl: './transaction-card-component.scss',
    imports: [CurrencyFormatPipe, MatCard, MatIcon, TranslatePipe],
    host: {
        role: 'button',
        tabindex: '0',
        '[attr.aria-label]': 'transaction().name',
        '(keydown.enter)': 'onKeydownActivate($event)',
        '(keydown.space)': 'onKeydownActivate($event)',
    },
})
export default class TransactionCardComponent {
    private readonly elementRef = inject(ElementRef<HTMLElement>);

    /**
     * Megjelenítendő tranzakció adatai
     */
    public transaction = input.required<TransactionListElementData>();

    /**
     * Billentyűzettel is aktiválható legyen a kártya (Enter/Space), ugyanúgy mint egérkattintásra
     */
    protected onKeydownActivate(event: Event): void {
        event.preventDefault();
        this.elementRef.nativeElement.click();
    }

    /**
     * Tranzakció típusa bevétel-e
     */
    protected isIncome: Signal<boolean> = computed(
        () => this.transaction().transactionType === TransactionTypeEnum.INCOME,
    );

    /**
     * A tranzakció tételeinek kategóriái (egyediek), vesszővel elválasztva felsorolva
     */
    protected categories: Signal<string> = computed(() => {
        const categories = this.transaction().transactionDetails.flatMap(
            (detail) => detail.categories,
        );
        return [...new Set(categories)].join(', ');
    });
}
