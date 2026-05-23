import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { ThemeStore } from './core/theme/theme.store';
import { AuthFacade } from './features/auth/services/auth.facade';
import { ToastComponent } from './shared/components/toast/toast';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ToastComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  private readonly authFacade = inject(AuthFacade);
  private readonly themeStore = inject(ThemeStore);

  constructor() {
    this.themeStore.initialize();
    void this.authFacade.initialize();
  }
}
