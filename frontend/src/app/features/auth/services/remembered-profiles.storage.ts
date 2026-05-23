import { Injectable, signal } from '@angular/core';

import { CurrentUser } from '@app/features/auth/models/user.models';
import { RememberedProfile } from '@app/features/auth/models/auth.models';

const REMEMBERED_PROFILES_KEY = 'creative_saas.remembered_profiles';
const MAX_REMEMBERED_PROFILES = 6;

@Injectable({ providedIn: 'root' })
export class RememberedProfilesStorage {
  private readonly profilesSignal = signal<readonly RememberedProfile[]>(this.read());

  readonly profiles = this.profilesSignal.asReadonly();

  rememberUser(user: CurrentUser): void {
    const nextProfile: RememberedProfile = {
      id: user.id,
      name: user.fullName || user.name || user.email,
      email: user.email,
      avatarUrl: null,
      lastUsedAt: new Date().toISOString(),
    };

    const profiles = [
      nextProfile,
      ...this.profilesSignal().filter((profile) => profile.id !== user.id && profile.email !== user.email),
    ].slice(0, MAX_REMEMBERED_PROFILES);

    this.write(profiles);
  }

  removeProfile(profileId: string): void {
    this.write(this.profilesSignal().filter((profile) => profile.id !== profileId));
  }

  clear(): void {
    this.write([]);
  }

  private read(): readonly RememberedProfile[] {
    if (!this.available()) {
      return [];
    }

    const raw = localStorage.getItem(REMEMBERED_PROFILES_KEY);
    if (!raw) {
      return [];
    }

    try {
      const parsed = JSON.parse(raw) as unknown;
      if (!Array.isArray(parsed)) {
        return [];
      }

      return parsed
        .map((item) => this.normalizeProfile(item))
        .filter((profile): profile is RememberedProfile => profile !== null)
        .sort((left, right) => right.lastUsedAt.localeCompare(left.lastUsedAt))
        .slice(0, MAX_REMEMBERED_PROFILES);
    } catch {
      return [];
    }
  }

  private write(profiles: readonly RememberedProfile[]): void {
    this.profilesSignal.set(profiles);

    if (!this.available()) {
      return;
    }

    if (profiles.length === 0) {
      localStorage.removeItem(REMEMBERED_PROFILES_KEY);
      return;
    }

    localStorage.setItem(REMEMBERED_PROFILES_KEY, JSON.stringify(profiles));
  }

  private normalizeProfile(value: unknown): RememberedProfile | null {
    if (!value || typeof value !== 'object') {
      return null;
    }

    const candidate = value as Partial<RememberedProfile>;
    if (
      typeof candidate.id !== 'string' ||
      typeof candidate.name !== 'string' ||
      typeof candidate.email !== 'string' ||
      typeof candidate.lastUsedAt !== 'string'
    ) {
      return null;
    }

    return {
      id: candidate.id,
      name: candidate.name,
      email: candidate.email,
      avatarUrl: typeof candidate.avatarUrl === 'string' ? candidate.avatarUrl : null,
      lastUsedAt: candidate.lastUsedAt,
    };
  }

  private available(): boolean {
    return typeof localStorage !== 'undefined';
  }
}
