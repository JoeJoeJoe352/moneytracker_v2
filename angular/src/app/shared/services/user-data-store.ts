import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class UserDataStore {
  /**
   * name of the user
   */
  public username = signal('');
  /**
   * Store already loaded? 
   */
  public isLoaded = signal(false);

  /**
   * The user is logged in?
   *
   * @returns
   */
  public isUserLogged(): boolean {
    return this.isLoaded() && this.username() !== '';
  }

  /**
   * Reset data (when logged out)
   */
  public resetData(): void {
    this.username.set('');
    this.isLoaded.set(false);
  }

  /**
   * User betöltése
   * 
   * @param String username 
   */
  public loadUser(username: string): void {
    this.username.set(username);
    this.isLoaded.set(true);
  }
}
