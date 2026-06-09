import { Component, signal } from "@angular/core";
import { LoginComponent } from "../../auth/components/loginComponent";
import { RegisterModalComponent } from "../../auth/components/registerModal";

@Component({
    selector: "app-welcome",
    templateUrl: "../pages/welcome.html",
    imports: [LoginComponent, RegisterModalComponent],
    standalone: true,
})
export class Welcome {
    isModalOpen = signal(false);

    openModal() {
        this.isModalOpen.set(true);
    }

    closeModal() {
        this.isModalOpen.set(false);
    }
}