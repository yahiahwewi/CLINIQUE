# Hospital Management System

Hospital Management System is a role-based full-stack application built for Sprint 1 evaluation. It combines a Spring Boot backend, an Angular frontend, containerized deployment, GitHub Actions CI, Kubernetes manifests, and starter monitoring assets.

## Architecture

- Frontend: Angular 21 SPA served by Nginx
- Backend: Spring Boot 3.4 REST API with Spring Security, JWT, JPA, Validation, Actuator
- Database: H2 for local development, PostgreSQL for Docker/Kubernetes deployment
- Monitoring: Spring Boot Actuator + Prometheus + Grafana
- DevOps: Docker, Docker Compose, GitHub Actions, Kubernetes manifests

## Roles And Features

- `ROLE_USER`
  - Register and login
  - Create, edit, and delete personal appointments
  - Request password reset and set a new password with a secure token
- `ROLE_DOCTOR`
  - View assigned appointments
  - Accept or delete assigned appointments
- `ROLE_NURSE`
  - View assigned appointments
- `ROLE_ADMIN`
  - Manage accounts
  - Manage appointments
  - Land on the admin overview after login

## Repository Structure

- `backend/`: Spring Boot API, security, DTOs, tests, Dockerfile
- `frontend/`: Angular UI, guards, interceptors, tests, Dockerfile
- `.github/workflows/`: CI pipelines for backend and frontend
- `k8s/`: Kubernetes manifests
- `monitoring/`: Prometheus and Grafana starter config
- `docker-compose.yml`: local container stack
- `docker-compose.prod.yml`: production-oriented stack with monitoring

## Default Demo Accounts

- Admin: `admin@example.com` / `admin123`
- Patient: `john@example.com` / `password123`
- Doctor: `doctor@example.com` / `password123`
- Nurse: `nurse@example.com` / `password123`

## Local Development

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend URL: `http://localhost:18081/api`

Useful endpoints:

- Health: `http://localhost:18081/api/actuator/health`
- Prometheus metrics: `http://localhost:18081/api/actuator/prometheus`
- H2 console: `http://localhost:18081/api/h2-console`

### Frontend

```bash
cd frontend
npm ci
npm start
```

Frontend URL: `http://localhost:4200`

The Angular dev server proxies `/api` to the backend automatically.

## Forgot Password Flow

1. Open `/forgot-password`
2. Submit the account email
3. Check the backend logs for the mock reset token
4. Open `/reset-password`
5. Paste the token and set the new password

Backend endpoints:

- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

## Testing

### Backend

```bash
cd backend
./mvnw test
```

Coverage includes:

- Auth service logic
- Auth controller
- User controller
- Appointment controller
- Security access rules

### Frontend

```bash
cd frontend
npm ci
npm run test:ci
```

Coverage includes:

- Auth service
- Login component
- Routing configuration
- Root app shell

## Docker

### Development Stack

```bash
cp .env.example .env
docker compose up --build -d
```

Default app URL: `http://localhost:8080`

### Production-Oriented Stack

```bash
cp .env.example .env
docker compose -f docker-compose.prod.yml up --build -d
```

Included services:

- frontend
- backend
- postgres
- prometheus
- grafana

Environment values come from `.env`. Do not hardcode production secrets.

## Monitoring

Prometheus config is available in `monitoring/prometheus.yml`.

Grafana starter assets:

- Datasource provisioning: `monitoring/grafana/provisioning/datasources/datasource.yml`
- Dashboard provisioning: `monitoring/grafana/provisioning/dashboards/dashboard.yml`
- Sample dashboard: `monitoring/grafana/dashboards/spring-boot-observability.json`

Default ports:

- Prometheus: `9090`
- Grafana: `3000`

## Kubernetes

Apply the manifests in this order:

```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/database-deployment.yaml
kubectl apply -f k8s/backend-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml
kubectl apply -f k8s/services.yaml
```

Files included:

- `k8s/configmap.yaml`
- `k8s/secrets.yaml`
- `k8s/database-deployment.yaml`
- `k8s/backend-deployment.yaml`
- `k8s/frontend-deployment.yaml`
- `k8s/services.yaml`

Update image names and secret values before deploying to a real cluster.

## CI/CD

GitHub Actions workflows:

- `.github/workflows/backend.yml`
- `.github/workflows/frontend.yml`

What they do:

- Install dependencies with caching
- Run tests
- Build application artifacts
- Build Docker images
- Fail fast on any error

## API Summary

Auth:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

Users/Admin:

- `GET /api/users/me`
- `GET /api/admin/users`
- `POST /api/admin/users`
- `PATCH /api/admin/users/{id}/toggle-status`
- `PATCH /api/admin/users/{id}/change-password`

Appointments:

- `GET /api/appointments`
- `GET /api/appointments/meta`
- `POST /api/appointments`
- `PUT /api/appointments/{id}`
- `PATCH /api/appointments/{id}/status`
- `DELETE /api/appointments/{id}`

## Branching Strategy

Recommended Git workflow for the evaluation:

- `main`: stable branch for releases and demos
- `dev`: integration branch for validated work
- `feature/*`: short-lived feature branches for focused tasks

## Deployment Notes

- Set a strong `APP_JWT_SECRET`
- Change database credentials
- Restrict `APP_CORS_ALLOWED_ORIGINS`
- Replace mock password reset delivery with real email integration before production use
