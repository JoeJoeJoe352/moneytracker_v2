import { Component, HostListener, inject } from '@angular/core';
import { AuthService } from '../../features/auth/auth-service';
import { Router, RouterLink } from '@angular/router';
import { UserDataStore } from '../services/user-data-store';

@Component({
  selector: 'app-header',
  templateUrl: 'header.html',
  styleUrl: './header.scss',
  imports: [RouterLink],
})
export class Header {
  private authService = inject(AuthService);
  private router = inject(Router);
  protected userData = inject(UserDataStore)

  /**
   * Hamburger menu open
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
        this.userData.resetData()
        this.router.navigate(["/welcome"]) // ez az utolsó mindig
      },
      error: (response) => {
        console.error(response)
        //TODO: új toast: this.snackbar.open('Error during logout', 'close');
      }
    });
  }
}
