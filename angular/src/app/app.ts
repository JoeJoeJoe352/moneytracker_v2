import { Component, inject, signal, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Header } from './shared/components/header';
import { Footer } from './shared/components/footer';
import { ReactiveFormsModule } from '@angular/forms';
import { AuthService } from './features/auth/auth-service';
import { UserDataStore } from './shared/services/user-data-store';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Header, Footer, ReactiveFormsModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit {
  protected readonly title = signal('Moneytracker');
  private authService = inject(AuthService);
  private userDataStore = inject(UserDataStore)

  ngOnInit() {
    // app indításkor (pl.: f5 után) authentikáljuk a usert, hogy be van-e jelentkezve
    this.authService.authenticateUser().subscribe({
      next: (response) => {
        this.userDataStore.username.set(response.username)
        this.userDataStore.isLoaded.set(true)
      },
      error: (error) => {
        if (error.status !== 401) {
          console.error('unknown error during authcheck!', error);
        }
      },
    });
  }
}
