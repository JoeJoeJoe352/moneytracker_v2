import { Component, HostListener, inject } from '@angular/core';
import { AuthService } from '../../features/auth/auth-service';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-header',
  templateUrl: 'header.html',
  styleUrl: './header.scss',
})
export class Header {
  private authService = inject(AuthService);
  private router = inject(Router);
  private snackbar = inject(MatSnackBar);
  /**
   * Is hamburger menu open
   */
  protected menuOpen = false;
  
  @HostListener('document:keydown.escape')
  onEsc() {
    this.menuOpen = false;
  }

  /**
   * hamburger menu open/close
   */
  toggleMenu(): void {
    this.menuOpen = !this.menuOpen;
  }

  /**
   * user logout
   */
  logout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(["/"])
      },
      error: (response) => {
        console.error(response)
        this.snackbar.open('Error during logout', 'close');
      }
    });
  }
}
