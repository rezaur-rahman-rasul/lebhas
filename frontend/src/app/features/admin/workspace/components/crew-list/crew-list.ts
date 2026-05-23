import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { CardComponent } from '@app/shared/components/card/card';
import { IconComponent } from '@app/shared/components/icon/icon';
import { CrewMember, crewPermissionSummary } from '../../models/crew.models';

@Component({
  selector: 'app-crew-list',
  standalone: true,
  imports: [DatePipe, BadgeComponent, CardComponent, IconComponent],
  templateUrl: './crew-list.html',
  styleUrl: './crew-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CrewListComponent {
  readonly members = input.required<readonly CrewMember[]>();
  readonly canManage = input(false);
  readonly removingCrewId = input<string | null>(null);
  readonly editRequested = output<CrewMember>();
  readonly removeRequested = output<CrewMember>();

  protected memberName(member: CrewMember): string {
    const fullName = [member.firstName, member.lastName].filter(Boolean).join(' ').trim();
    return fullName || member.email;
  }

  protected permissionSummary(member: CrewMember): string {
    return crewPermissionSummary(member.permissions);
  }

  protected statusLabel(status: CrewMember['status']): string {
    switch (status) {
      case 'ACTIVE':
        return 'Active';
      case 'INVITED':
        return 'Invited';
      case 'SUSPENDED':
        return 'Suspended';
      case 'REVOKED':
        return 'Removed';
      default:
        return status;
    }
  }

  protected statusTone(status: CrewMember['status']): 'brand' | 'blue' | 'red' | 'neutral' {
    switch (status) {
      case 'ACTIVE':
        return 'brand';
      case 'INVITED':
        return 'blue';
      case 'SUSPENDED':
        return 'red';
      default:
        return 'neutral';
    }
  }
}
