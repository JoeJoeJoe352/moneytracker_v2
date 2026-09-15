import {
    Component,
    computed,
    ElementRef,
    HostListener,
    inject,
    Input,
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
        '[attr.aria-label]': 'this.transaction.name',
    },
})
export default class TransactionCardComponent {
    private readonly elementRef = inject(ElementRef<HTMLElement>);

    /**
     * Megjelenítendő tranzakció adatai
     */
    @Input({ required: true }) transaction!: TransactionListElementData;

    /**
     * Billentyűzettel is aktiválható legyen a kártya (Enter/Space), ugyanúgy mint egérkattintásra
     */
    @HostListener('keydown.enter', ['$event'])
    @HostListener('keydown.space', ['$event'])
    protected onKeydownActivate(event: Event): void {
        event.preventDefault();
        this.elementRef.nativeElement.click();
    }

    /**
     * Tranzakció típusa bevétel-e
     */
    protected isIncome: Signal<boolean> = computed(
        () => this.transaction.transactionType == TransactionTypeEnum.INCOME,
    );

    /**
     * Tranzakció kategóriák listája
     */
    protected categoryList: Signal<Set<string>> = computed(() => {
        const categories: string[] = this.transaction.transactionDetails.flatMap(
            (detail) => detail.categories,
        );
        return new Set(categories);
    });

    /**
     * Kategóriák listája felsorolva, vesszővel elválasztva
     */
    protected categoriesAsString: Signal<string> = computed(() => {
        return Array.from(this.categoryList()).join(', ');
    });
}
