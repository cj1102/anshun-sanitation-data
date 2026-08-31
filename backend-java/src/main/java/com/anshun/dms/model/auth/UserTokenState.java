package com.anshun.dms.model.auth;

/** Database-backed security stamp used to revoke JWTs after role or status changes. */
public record UserTokenState(long userId, String username, String status, int tokenVersion) { }
