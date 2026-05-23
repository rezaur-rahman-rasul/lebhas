import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { CREW_PERMISSION_OPTIONS, CrewPermission } from '../../models/crew.models';

@Component({
  selector: 'app-crew-permission-panel',
  standalone: true,
  templateUrl: './crew-permission-panel.html',
  styleUrl: './crew-permission-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CrewPermissionPanelComponent {
  readonly title = input('Permissions');
  readonly description = input('Decide what the crew member can do inside this workspace.');
  readonly selectedPermissions = input<readonly CrewPermission[]>(['WORKSPACE_VIEW']);
  readonly disabled = input(false);
  readonly error = input('');
  readonly selectionChange = output<readonly CrewPermission[]>();

  protected readonly permissionOptions = CREW_PERMISSION_OPTIONS;

  protected toggle(permission: CrewPermission, enabled: boolean): void {
    if (this.disabled()) {
      return;
    }

    const next = new Set(this.selectedPermissions());
    if (enabled) {
      next.add(permission);
    } else {
      next.delete(permission);
    }

    this.selectionChange.emit(
      this.permissionOptions
        .map((option) => option.permission)
        .filter((optionPermission) => next.has(optionPermission)),
    );
  }
}
