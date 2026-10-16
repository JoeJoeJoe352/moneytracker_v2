import { formatDate } from '@angular/common';
import { Injectable } from '@angular/core';

@Injectable({
    providedIn: 'root',
})
export class TimeHelper {
    /**
     * Date objectet, Y-m-d-re formázott stringgé alakítja
     */
    public formatDateToString(date: Date): string {
        return formatDate(date, 'yyyy-MM-dd', 'en-US');
    }

    /**
     * Date stringet átalakítja egy Date object-é
     * nem elég new Date(string), mert az UTC-ként értelmezné a dátumot
     */
    public stringToDate(dateString: string): Date {
        const isDateStringValid = /^(\d{4})-(\d{2})-(\d{2})$/.exec(dateString ?? '');
        if (!isDateStringValid) {
            console.error('invalid dateString: ' + dateString);
            return new Date();
        }

        const [year = 0, month = 1, day = 1] = dateString.split('-').map(Number);
        const date = new Date(year, month - 1, day);

        if (date.getMonth() !== month - 1) {
            // pl. 2026-02-31 -> március 3. lenne
            console.error('invalid dateString: ' + dateString);
            return new Date();
        }
        return date;
    }
}
