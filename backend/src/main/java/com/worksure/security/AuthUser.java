package com.worksure.security;

public record AuthUser(long id, String email, String role, String fullName) {}
