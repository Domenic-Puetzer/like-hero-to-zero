# Project Architecture and Technical Overview

This document provides an overview of the architecture, main components, and technical decisions of the Like Hero To Zero project.

## Overview

Like Hero To Zero is a web application for visualizing and managing global CO₂ emissions data. It consists of a Java Spring Boot backend and a Thymeleaf/JavaScript frontend.

## Main Components

- **Backend:** Java 21, Spring Boot, Spring Data JPA, Spring Security
- **Frontend:** Thymeleaf templates, Tailwind CSS, Vanilla JS, Leaflet.js
- **Database:** MySQL (H2 for development)

## Architecture Diagram

```
[User] <-> [Frontend (Thymeleaf, JS, CSS)] <-> [Spring Boot Backend] <-> [Database]
```

## Key Modules

- **Controller Layer:** Handles HTTP requests and routes.
- **Service Layer:** Business logic and data processing.
- **Repository Layer:** Data access with JPA.
- **Model Layer:** Entity definitions.

## Data Flow

1. User interacts with the frontend (map, forms, etc.).
2. Frontend sends requests to backend endpoints.
3. Backend processes requests, interacts with the database, and returns data.
4. Frontend updates the UI accordingly.

## Security

- User authentication and role-based access (User, Scientist, Admin planned).
- Spring Security for session management.

## Extensibility

- Modular structure for easy feature addition.
- REST API endpoints for data access.

---

For more details, see the codebase and other documentation files.
