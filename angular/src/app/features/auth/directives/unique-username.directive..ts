import { inject, Injectable } from "@angular/core";
import { AbstractControl, AsyncValidator, ValidationErrors } from "@angular/forms";
import { catchError, map, Observable, of } from "rxjs";
import { AuthService } from "../services/auth-service";

@Injectable({providedIn: 'root'})
export class UniqueNameAndEmailDirective {
    private readonly userService = inject(AuthService)

      validateUsername(control: AbstractControl) {
    return this.userService.checkNameUniqueness(control.value).pipe(
      map(isTaken => isTaken ? { usernameTaken: true } : null),
      catchError(() => of(null))
    );
  }

  validateEmail(control: AbstractControl) {
    return this.userService.checkEmailUniqueness(control.value).pipe(
      map(isTaken => isTaken ? { emailTaken: true } : null),
      catchError(() => of(null))
    );
  } 
}