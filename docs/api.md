# API Documentation (REST Endpoints)

This document describes the main REST API endpoints for Like Hero To Zero.

## Base URL

    http://localhost:8080/api/

## Endpoints

### Public

- `GET /api/emissions` — List all emission data
- `GET /api/emissions/{country}` — Get emission data for a country

### Authenticated (User/Scientist)

- `POST /api/auth/login` — Log in
- `POST /api/auth/register` — Register

### Scientist

- `POST /api/emissions` — Upload new emission data
- `PUT /api/emissions/{id}` — Edit emission data
- `POST /api/edit-requests` — Propose data edits
- `GET /api/edit-requests` — List edit requests

### (Planned) Admin

- `POST /api/approve` — Approve/reject submissions (to be done)

## Notes

- All endpoints return JSON.
- Authentication required for non-public endpoints.

---

For detailed models and request/response formats, see the codebase or contact the maintainers.
