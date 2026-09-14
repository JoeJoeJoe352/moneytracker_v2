import { Component } from "@angular/core";
import { TranslatePipe } from "@ngx-translate/core";
import { MatDivider } from "@angular/material/divider";
import { MatButton } from "@angular/material/button";

@Component({
  selector: "app-footer",
  templateUrl: "footer.html",
  styleUrl: "./footer.scss",
  imports: [TranslatePipe, MatDivider, MatButton],
})
export class Footer {
  
  /**
   * Visszatér az aktuális dátummal
   */
  getYear(): number {
    return new Date().getFullYear();
  }
}