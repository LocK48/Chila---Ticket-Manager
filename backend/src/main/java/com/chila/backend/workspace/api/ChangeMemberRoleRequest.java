package com.chila.backend.workspace.api;

import com.chila.backend.workspace.MembershipRole;
import jakarta.validation.constraints.NotNull;

public record ChangeMemberRoleRequest(@NotNull MembershipRole role) {
}
