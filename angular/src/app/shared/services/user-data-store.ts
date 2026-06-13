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
    return this.username() != '';
  }

  /**
   * Reset data (when logged out)
   */
  public resetData(): void {
    this.username.set('');
    this.isLoaded.set(false);
  }
}
