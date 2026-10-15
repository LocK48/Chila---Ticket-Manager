# Chila — Team Project & Issue Management Platform

Chila is a full-stack project and issue management platform built with Java, Spring Boot, PostgreSQL, React, and TypeScript. It helps development teams organize projects, assign tasks, manage workflows, and track progress in a shared workspace.

## Features

- Authentication and authorization with Spring Security and JWT
- Workspace and membership management with role-based access control
- Project and issue management
- Kanban board with issue status and priority
- Issue assignment, comments, and activity history
- Search, filtering, sorting, and pagination
- REST API validation and centralized exception handling
- Automated tests and Docker-based local development

## Tech Stack

**Backend**

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA and Hibernate
- Spring Security
- Maven

**Frontend**

- React
- TypeScript
- Vite
- Tailwind CSS

**Database and DevOps**

- PostgreSQL
- Flyway
- Docker Compose
- GitHub Actions
- JUnit 5, Mockito, and Testcontainers

## Architecture

Chila uses a modular monolith architecture. The backend separates controllers, services, repositories, entities, and DTOs.

The frontend communicates with the backend through a versioned REST API. PostgreSQL stores users, workspaces, memberships, projects, issues, and comments.

See `docs/architecture.md` for the architecture diagram and design decisions.

## Getting Started

### Prerequisites

- Java 21
- Maven
- Node.js and npm
- Docker and Docker Compose
- Git

### Installation

1. Clone the repository:

   `git clone https://github.com/YOUR_USERNAME/chila.git`

2. Enter the project directory:

   `cd chila`

3. Create your local environment configuration using `.env.example` as a reference. Set database credentials and JWT configuration. Never commit real secrets.

4. Start the PostgreSQL service:

   `docker compose up -d postgres`

5. Start the backend using Maven:

   `cd backend && mvn spring-boot:run`

6. In a separate terminal, install frontend dependencies and start the development server:

   `cd frontend && npm install && npm run dev`

7. Open the frontend URL printed by Vite and test the application.

Update these commands to match the final project configuration.

## API Documentation

When the backend is running, open the configured Swagger UI endpoint to explore the REST API.

API examples and authentication instructions are available in `docs/api-examples.md`.

## Testing

Run backend verification:

`cd backend && mvn verify`

Run frontend checks:

`cd frontend && npm run lint`

Add the relevant frontend test command once the test runner is configured.

## Database Design

The main entities are User, Workspace, Membership, Project, Issue, Comment, and IssueHistory.

Database schema changes are managed through versioned Flyway migrations.

See `docs/architecture.md` for the ERD and data model details.

## Deployment

Configure production environment variables, deploy the backend and frontend, and connect the backend to a managed PostgreSQL instance.

The live demo and screenshots will be added here after deployment.

## Future Improvements

- Refresh token rotation and revocation
- Realtime notifications
- Redis caching for selected read-heavy endpoints
- Advanced reporting and issue analytics
- Improved observability and performance testing

## License

This project is licensed under the MIT License. See `LICENSE` for details.
