import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-switch',
  templateUrl: './switch.component.html',
  styleUrls: ['./switch.component.scss'],
  standalone: true
})
export class SwitchComponent {
  @Input() checked = false;
  @Input() labelBefore = '';
  @Input() labelAfter = '';

  @Output() checkedChange = new EventEmitter<boolean>();

  toggle(event: Event) {
    const value = (event.target as HTMLInputElement).checked;
    this.checked = value;
    this.checkedChange.emit(value);
  }
}