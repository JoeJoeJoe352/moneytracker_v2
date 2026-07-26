import { Component, Input, forwardRef } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
    selector: 'app-switch',
    templateUrl: './switch.component.html',
    styleUrls: ['./switch.component.scss'],
    standalone: true,
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SwitchComponent),
            multi: true,
        },
    ],
})
export class SwitchComponent {
    @Input() checked = false;
    @Input() labelBefore = '';
    @Input() labelAfter = '';

    // angular formcontrol-hoz kellő definiciók, függvények, stb...

    private onChange: (value: boolean) => void = () => undefined;
    private onTouched: () => void = () => undefined;

    writeValue(value: boolean): void {
        this.checked = value;
    }

    registerOnChange(fn: (value: boolean) => void): void {
        this.onChange = fn;
    }

    registerOnTouched(fn: () => void): void {
        this.onTouched = fn;
    }

    setChecked(value: boolean) {
        this.checked = value;
        this.onChange(value);
        this.onTouched();
    }
}
