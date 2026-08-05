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
- `date`: assignment date
- `startTime`: shift start time
- `endTime`: shift end time
- `totalHours`: total worked hours for the shift
- `notes`: optional notes

## Relationships

- One `Guard` can have many `ShiftAssignment` records.
- One `Service` can have many `ShiftAssignment` records.
- Each `ShiftAssignment` belongs to exactly one `Guard`.
- Each `ShiftAssignment` belongs to exactly one `Service`.

## Naming

- All model names and fields are in English.
