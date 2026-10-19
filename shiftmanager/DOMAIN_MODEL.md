# Initial Domain Model

This document describes the initial backend domain model.

## Models

### User
- `id`: unique identifier
- `username`: unique username
- `email`: unique email
- `passwordHash`: hashed password
- `active`: account status

### Guard
- `id`: unique identifier
- `firstName`: guard first name
- `lastName`: guard last name
- `dni`: unique national identification document
- `phone`: contact phone
- `active`: guard status

### Service
- `id`: unique identifier
- `name`: service name
- `location`: physical location
- `description`: service details
- `active`: service status

### ShiftAssignment
- `id`: unique identifier
- `guard`: assigned guard
- `service`: assigned service
- `startDateTime`: planned shift start (date and time)
- `endDateTime`: planned shift end (date and time). It can be on the next day, e.g. a night shift from 20:00 to 08:00.
- `totalHours`: worked hours, calculated by the server. It uses the real check-in/check-out times when both exist, otherwise the planned duration.
- `notes`: optional notes
- `actualStartTime`: real check-in time (nullable)
- `actualEndTime`: real check-out time (nullable)

## Business rules

- `endDateTime` must be after `startDateTime` and a shift cannot be longer than 24 hours.
- A guard cannot have overlapping shifts. Back-to-back shifts (one ends 08:00, the next starts 08:00) are allowed.
- New shifts can only be assigned to active guards and active services.
- Guards and services are never deleted, only deactivated (`active = false`), to keep the shift history.

## Relationships

- One `Guard` can have many `ShiftAssignment` records.
- One `Service` can have many `ShiftAssignment` records.
- Each `ShiftAssignment` belongs to exactly one `Guard`.
- Each `ShiftAssignment` belongs to exactly one `Service`.

## Naming

- All model names and fields are in English.
