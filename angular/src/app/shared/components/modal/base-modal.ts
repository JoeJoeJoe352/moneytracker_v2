import { FocusTrap, FocusTrapFactory } from "@angular/cdk/a11y";
import { AfterViewInit, Component, ElementRef, EventEmitter, HostListener, inject, Input, OnDestroy, Output } from "@angular/core";

@Component({
    selector: "app-base-modal",
    templateUrl: "./base-modal.html",
    styleUrls: ["./modal.scss"],
})
export class BaseModal implements AfterViewInit, OnDestroy {
    @Input({ required: true }) title = '';
    @Output() closeModal = new EventEmitter<boolean>();
    // Injektált elemek
    private focusTrapFactory = inject(FocusTrapFactory)
    private el = inject(ElementRef)
    /**
     * Modal felnyitása esetén garantálja, hogy a tab gomb nem tud kiszökni a modal területéről
     * ngAfterViewInit-ben inicializálódik
     */
    private focusTrap!: FocusTrap;
    
    // esc lenyomását figyeli
    @HostListener('document:keydown.escape')

    // automatikusan meghívja az onEsc függvényt, ha esc gomb lenyomásra kerül
    onEsc() {
        this.closeModal.emit();
    }

    onClose() {
        this.closeModal.emit();
    }

    ngAfterViewInit() {
        const modalRoot = this.el.nativeElement.querySelector('.modal-content');
        if (!modalRoot) {
            console.error('Modal content element not found');
            return;
        }
        this.focusTrap = this.focusTrapFactory.create(modalRoot);
        this.focusTrap.focusInitialElement();
    }

    ngOnDestroy() {
        // Ha nincs modal-content elem, akkor lehet null a focustrap
        this.focusTrap?.destroy();
    }
}