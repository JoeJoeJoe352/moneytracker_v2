import { Component } from "@angular/core";
import { baseModal } from "../../../shared/components/modal/base-modal";
import { RegisterComponent } from "./RegisterComponent";

@Component({
    selector: "register-modal",
    templateUrl: "./registerModal.html",
    imports: [baseModal, RegisterComponent],
})
export class registerModal {
    isRegisterModalShown: boolean = false
}