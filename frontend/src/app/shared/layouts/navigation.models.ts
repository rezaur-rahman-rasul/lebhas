export interface NavigationItem {
  readonly label: string;
  readonly icon: string;
  readonly route: string;
  readonly description?: string;
  readonly requiresWorkspace?: boolean;
}

