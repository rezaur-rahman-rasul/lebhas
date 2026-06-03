import { HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { ApiEndpoints } from '@app/core/api/api-endpoints';
import {
  CrewInvitation,
  CrewMember,
  InviteCrewPayload,
  UpdateCrewMemberPayload,
} from '../models/crew.models';

interface CrewMemberResponseDto {
  readonly userId: string;
  readonly workspaceId: string;
  readonly firstName: string | null;
  readonly lastName: string | null;
  readonly email: string;
  readonly phone: string | null;
  readonly role: CrewMember['role'];
  readonly status: CrewMember['status'];
  readonly permissions: readonly CrewMember['permissions'][number][];
  readonly joinedAt: string;
  readonly invitedByUserId: string | null;
  readonly lastLoginAt: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

interface CrewInvitationResponseDto {
  readonly invitationToken: string;
  readonly workspaceId: string;
  readonly email: string;
  readonly role: CrewInvitation['role'];
  readonly permissions: readonly CrewInvitation['permissions'][number][];
  readonly expiresAt: string;
  readonly status: string;
}

@Injectable({ providedIn: 'root' })
export class CrewService {
  private readonly api = inject(ApiService);

  inviteCrew(workspaceId: string, payload: InviteCrewPayload) {
    return this.api
      .post<CrewInvitationResponseDto, InviteCrewPayload>(
        ApiEndpoints.workspaces.memberInvite(workspaceId),
        payload,
      )
      .pipe(map(({ data }) => mapCrewInvitation(data)));
  }

  listCrew(workspaceId: string, context?: HttpContext) {
    return this.api
      .get<readonly CrewMemberResponseDto[]>(ApiEndpoints.workspaces.members(workspaceId), { context })
      .pipe(map(({ data }) => data.map(mapCrewMember)));
  }

  getCrewMember(workspaceId: string, crewId: string) {
    return this.api
      .get<CrewMemberResponseDto>(ApiEndpoints.workspaces.member(workspaceId, crewId))
      .pipe(map(({ data }) => mapCrewMember(data)));
  }

  updateCrewMember(workspaceId: string, crewId: string, payload: UpdateCrewMemberPayload) {
    return this.api
      .put<CrewMemberResponseDto, UpdateCrewMemberPayload>(
        ApiEndpoints.workspaces.member(workspaceId, crewId),
        payload,
      )
      .pipe(map(({ data }) => mapCrewMember(data)));
  }

  removeCrewMember(workspaceId: string, crewId: string) {
    return this.api.delete<void>(ApiEndpoints.workspaces.member(workspaceId, crewId));
  }
}

function mapCrewInvitation(source: CrewInvitationResponseDto): CrewInvitation {
  return {
    invitationToken: source.invitationToken,
    workspaceId: source.workspaceId,
    email: source.email,
    role: source.role,
    permissions: source.permissions ?? [],
    expiresAt: source.expiresAt,
    status: source.status,
  };
}

function mapCrewMember(source: CrewMemberResponseDto): CrewMember {
  return {
    id: source.userId,
    userId: source.userId,
    workspaceId: source.workspaceId,
    firstName: source.firstName,
    lastName: source.lastName,
    email: source.email,
    phone: source.phone,
    role: source.role,
    status: source.status,
    permissions: source.permissions ?? [],
    joinedAt: source.joinedAt,
    invitedByUserId: source.invitedByUserId,
    lastLoginAt: source.lastLoginAt,
    createdAt: source.createdAt,
    updatedAt: source.updatedAt,
  };
}
