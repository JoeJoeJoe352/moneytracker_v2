import { Component } from "@angular/core";
import { TranslatePipe } from "@ngx-translate/core";

@Component({
  selector: "app-footer",
  templateUrl: "footer.html",
  styleUrl: "./footer.scss",
  imports: [TranslatePipe],
})
export class Footer {
  
  /**
   * Visszatér az aktuális dátummal
   */
  getYear(): number {
    return new Date().getFullYear();
  }
}