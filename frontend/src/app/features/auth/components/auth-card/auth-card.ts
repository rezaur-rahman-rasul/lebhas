import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-auth-card',
  standalone: true,
  templateUrl: './auth-card.html',
  styleUrl: './auth-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthCardComponent {}
