import { Component } from "@angular/core";

@Component({
  selector: "app-footer",
  templateUrl: "footer.html",
  styleUrl: "./footer.scss",
})
export class Footer {
  
  /**
   * Visszatér az aktuális dátummal
   */
  getYear(): number {
    return new Date().getFullYear();
  }
}