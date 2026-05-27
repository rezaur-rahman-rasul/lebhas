import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import {
  safeMetadataPreview,
  summarizeIpAddress,
  summarizeUserAgent,
} from '../../services/sensitive-metadata';
import { AuditLog } from '../../models/audit.models';
import { AuditSeverityBadgeComponent } from '../audit-severity-badge/audit-severity-badge';

@Component({
  selector: 'app-audit-log-card',
  standalone: true,
  imports: [ButtonComponent, CardComponent, AuditSeverityBadgeComponent],
  templateUrl: './audit-log-card.html',
  styleUrl: './audit-log-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditLogCardComponent {
  readonly auditLog = input.required<AuditLog>();
  readonly detailSelected = output<AuditLog>();

  protected readonly actorLabel = computed(() => this.auditLog().actorName || 'System user');
  protected readonly referenceLabel = computed(() => auditReferenceLabel(this.auditLog()));
  protected readonly ipSummary = computed(() => summarizeIpAddress(this.auditLog().ipAddress));
  protected readonly userAgentSummary = computed(() => summarizeUserAgent(this.auditLog().userAgent));
  protected readonly metadataSummary = computed(
    () => safeMetadataPreview(this.auditLog().metadataJson) ?? 'No activity details',
  );
  protected readonly friendlyAuditText = friendlyAuditText;
}

export function auditReferenceLabel(auditLog: AuditLog): string {
  if (auditLog.referenceType && auditLog.referenceId) {
    return `${friendlyAuditText(auditLog.referenceType)} ${auditLog.referenceId}`;
  }

  if (auditLog.referenceType) {
    return friendlyAuditText(auditLog.referenceType);
  }

  return 'Workspace';
}

export function friendlyAuditText(value: string | null | undefined): string {
  if (!value) {
    return 'Not available';
  }

  return value
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}
