import { AbstractControl, ValidationErrors, ValidatorFn } from "@angular/forms";

export function passwordMismatchValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
        const password = control.get('password')?.value;
        const passwordAgain = control.get('passwordAgain')?.value;

        if (!password || !passwordAgain) {
            return null;
        }
        console.log(password, passwordAgain)
        return password === passwordAgain ? null : { passwordMismatch: true };
    };
}