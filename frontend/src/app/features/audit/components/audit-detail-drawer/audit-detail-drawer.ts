import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { AppDrawerComponent } from '@app/shared/components/app-drawer/app-drawer';
import {
  maskSensitiveMetadata,
  summarizeIpAddress,
  summarizeUserAgent,
} from '../../services/sensitive-metadata';
import { AuditLog } from '../../models/audit.models';
import { auditReferenceLabel, friendlyAuditText } from '../audit-log-card/audit-log-card';
import { AuditSeverityBadgeComponent } from '../audit-severity-badge/audit-severity-badge';

@Component({
  selector: 'app-audit-detail-drawer',
  standalone: true,
  imports: [AppDrawerComponent, AuditSeverityBadgeComponent],
  templateUrl: './audit-detail-drawer.html',
  styleUrl: './audit-detail-drawer.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditDetailDrawerComponent {
  readonly auditLog = input<AuditLog | null>(null);
  readonly closed = output<void>();

  protected readonly open = computed(() => Boolean(this.auditLog()));
  protected readonly actorLabel = computed(() => this.auditLog()?.actorName || 'System user');
  protected readonly referenceLabel = computed(() => {
    const auditLog = this.auditLog();
    return auditLog ? auditReferenceLabel(auditLog) : 'Workspace';
  });
  protected readonly ipSummary = computed(() => summarizeIpAddress(this.auditLog()?.ipAddress));
  protected readonly userAgentSummary = computed(() => summarizeUserAgent(this.auditLog()?.userAgent));
  protected readonly metadataPreview = computed(() => {
    const masked = maskSensitiveMetadata(this.auditLog()?.metadataJson);
    return masked ? formatMetadata(masked) : 'No activity details available.';
  });

  protected readonly friendlyAuditText = friendlyAuditText;
}

function formatMetadata(value: string): string {
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}
