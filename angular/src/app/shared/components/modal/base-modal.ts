import { Component, EventEmitter, Input, Output } from "@angular/core";

@Component({
    selector: "base-modal",
    templateUrl: "./base-modal.html",
    imports: [],
    styleUrls: ["./modal.scss"],
})
export class baseModal{
    @Input({ required: true }) title: string = '';
    @Output() close = new EventEmitter<boolean>();

    onClose() {
        this.close.emit();
    }
}