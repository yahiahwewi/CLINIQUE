# Hospital Management System

Role-based hospital scheduling app built with Spring Boot, Angular, and JWT authentication.

## Stack

- Backend: Spring Boot 3.4, Spring Security, JPA
- Frontend: Angular 21
- Database: H2 for local development, PostgreSQL for deployment
- Auth: JWT with role-based access control

## Roles

- `ROLE_USER`: create, edit, and delete their own appointments
- `ROLE_DOCTOR`: review assigned appointments and accept or cancel them
- `ROLE_NURSE`: view assigned appointments
- `ROLE_ADMIN`: manage users and appointments

## Local Development

### Backend

```bash
cd backend
mvn spring-boot:run
```

Backend default URL: `http://localhost:18081/api`

### Frontend

```bash
cd frontend
npm install
npm start
```

Frontend URL: `http://localhost:4200`

The Angular dev server now proxies `/api` requests to the backend automatically.

## Docker Deployment

```bash
cp .env.example .env
docker compose up --build -d
```

App URL: `http://localhost:8080`

If port `8080` is already in use, set `FRONTEND_PORT` in `.env` before starting the stack.

See [DEPLOYMENT.md](./DEPLOYMENT.md) for the full deployment flow.

## Default Test Accounts

- `john@example.com` / `password123`
- `doctor@example.com` / `password123`
- `nurse@example.com` / `password123`
- `admin@example.com` / `admin123`

## Notes

- Local development defaults to H2
- Container deployment uses PostgreSQL
- Set a real `APP_JWT_SECRET` before any public deployment
