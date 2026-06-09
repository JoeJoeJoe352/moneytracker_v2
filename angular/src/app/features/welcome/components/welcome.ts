import { Component, signal } from "@angular/core";
import { LoginComponent } from "../../auth/login-component";
import { RegisterModalComponent } from "../../auth/register-modal";

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