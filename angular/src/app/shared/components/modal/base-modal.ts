import { Component, EventEmitter, Input, Output } from "@angular/core";

@Component({
    selector: "base-modal",
    templateUrl: "./base-modal.html",
    imports: [],
    styleUrls: ["./modal.scss"],
})
export class baseModal{
    @Input({ required: true }) title: string = '';
    @Input({ required: true }) shown: boolean = false;
    @Output() shownChange = new EventEmitter<boolean>();

    closeModal() {
        this.shown = false;
        this.shownChange.emit(this.shown);
    }
}