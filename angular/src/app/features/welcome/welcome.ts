import { Component, inject, signal } from "@angular/core";
import { LoginComponent } from "../auth/login-component";
import { RegisterModalComponent } from "../auth/register-modal";
import { UserDataStore } from "../../shared/services/user-data-store";

@Component({
    selector: "app-welcome",
    templateUrl: "./welcome.html",
    imports: [LoginComponent, RegisterModalComponent],
    standalone: true,
})
export class Welcome {
    protected userData = inject(UserDataStore)

    isModalOpen = signal(false);

    openModal() {
        this.isModalOpen.set(true);
    }

    closeModal() {
        this.isModalOpen.set(false);
    }
}