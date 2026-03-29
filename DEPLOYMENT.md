# Deployment Guide

## Docker Compose

1. Copy the sample environment file:

```bash
cp .env.example .env
```

2. Update the secrets in `.env` before deploying anywhere public:

- `POSTGRES_PASSWORD`
- `FRONTEND_PORT`
- `APP_JWT_SECRET`
- `APP_CORS_ALLOWED_ORIGINS`

3. Build and start the stack:

```bash
docker compose up --build -d
```

4. Open the app:

- Frontend: `http://localhost:${FRONTEND_PORT}`

The frontend proxies `/api` requests to the backend container, so the browser only needs the frontend URL.

## Services

- `frontend`: Angular app served by Nginx
- `backend`: Spring Boot API
- `postgres`: PostgreSQL database with persisted volume storage

## Environment Variables

The backend now supports environment-driven deployment settings:

- `SERVER_PORT`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_DRIVER_CLASS_NAME`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_JPA_HIBERNATE_DDL_AUTO`
- `SPRING_H2_CONSOLE_ENABLED`
- `APP_JWT_SECRET`
- `APP_JWT_EXPIRATION`
- `APP_CORS_ALLOWED_ORIGINS`

## Local Development

- Frontend development still runs with `npm start`
- Backend development still defaults to H2 unless datasource variables are provided
- Angular now uses a proxy, so local API calls still work through `/api`

## Production Notes

- Replace the example JWT secret with a long random value
- Change `FRONTEND_PORT` if `8080` is already in use on the host
- Limit `APP_CORS_ALLOWED_ORIGINS` to your real frontend domains
- Use a managed PostgreSQL instance or secure persisted storage
- Put the stack behind HTTPS with a reverse proxy or load balancer
