package com.security.shiftmanager.model;

public enum Role {
    /** Can create, edit and delete guards, services, shifts and users. */
    ADMIN,
    /** Read only: can consult guards, services, shifts and reports. */
    SUPERVISOR
}
