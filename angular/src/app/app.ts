import { Component, inject, signal } from '@angular/core';
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
export class App {
  protected readonly title = signal('Moneytracker');
  private authService = inject(AuthService);
  private userDataStore = inject(UserDataStore)
}
