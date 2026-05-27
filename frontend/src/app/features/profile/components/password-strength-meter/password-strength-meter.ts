import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

type PasswordStrength = 'Weak' | 'Fair' | 'Good' | 'Strong';

@Component({
  selector: 'app-password-strength-meter',
  standalone: true,
  templateUrl: './password-strength-meter.html',
  styleUrl: './password-strength-meter.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PasswordStrengthMeterComponent {
  readonly password = input('');

  protected readonly score = computed(() => passwordScore(this.password()));
  protected readonly label = computed<PasswordStrength>(() => {
    const score = this.score();
    if (score >= 4) {
      return 'Strong';
    }
    if (score === 3) {
      return 'Good';
    }
    if (score === 2) {
      return 'Fair';
    }
    return 'Weak';
  });
  protected readonly meterClasses = computed(() => {
    const base = 'h-full rounded-full transition-all';
    switch (this.label()) {
      case 'Strong':
        return `${base} bg-success-500`;
      case 'Good':
        return `${base} bg-brand-500`;
      case 'Fair':
        return `${base} bg-warning-500`;
      default:
        return `${base} bg-alert-500`;
    }
  });
  protected readonly width = computed(() => Math.max(20, this.score() * 25));
}

function passwordScore(password: string): number {
  let score = 0;
  if (password.length >= 8) {
    score += 1;
  }
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) {
    score += 1;
  }
  if (/\d/.test(password)) {
    score += 1;
  }
  if (/[^A-Za-z0-9]/.test(password)) {
    score += 1;
  }
  return score;
}
