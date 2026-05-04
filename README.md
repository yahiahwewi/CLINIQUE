# Lumen Health

A role-based hospital management platform — Spring Boot + Angular 21 + a
standalone Python ML service. Patients self-book real availability slots,
doctors run a clinical workspace (medical record + prescriptions w/ PDF +
lab orders + referrals), an admin governs the whole thing, and an LLM
(Groq) plus a scikit-learn classifier provide AI assistance.

> Originally scaffolded as “Hospital Management System” for Sprint 1, then
> rebranded **Lumen Health** with a deep-teal design system and expanded into
> a clinical-workflow demo.

## Architecture

| Component | Stack | Port |
|---|---|---|
| **Backend** | Spring Boot 3.4, Spring Security + JWT, JPA/Hibernate, Validation, Actuator, OpenPDF | `:18081` |
| **Frontend** | Angular 21 standalone components, Tailwind 4, RxJS, Reactive Forms | `:4200` (dev) |
| **Lab-AI** | Flask 3, scikit-learn (RandomForest), pdfplumber, reportlab | `:5001` |
| **DB** | H2 in-memory (dev) / PostgreSQL (Docker, K8s) | — |
| **AI (LLM)** | Groq (`llama-3.3-70b-versatile`) — symptom triage + visit summary | external |
| **Monitoring** | Prometheus + Grafana | `:9090` / `:3000` |
| **DevOps** | Docker Compose, GitHub Actions, Kubernetes manifests | — |

## Roles and features

### `ROLE_USER` (Patient — auto-approved on signup)
- Sign up directly, sign in immediately
- Edit personal medical profile (DOB, gender, blood type, allergies, conditions, emergency contact)
- Browse doctors and book real availability slots
- AI symptom helper suggests department + drafts chief complaint
- View visit history timeline (past visits + AI-generated summaries + prescriptions w/ PDF + lab results)
- In-app notification bell + announcement banners

### `ROLE_DOCTOR` (admin-approved)
- Manage profile (specialty, departments, license, bio, languages, fee, photo)
- Define recurring weekly availability windows (per-day slot length)
- Request time off (auto-blocks bookable slots once admin approves)
- Per-appointment **clinical workspace**:
  - Medical record (chief complaint, diagnosis, plan, private notes)
  - **AI patient summary** — Groq turns terse clinical notes into a plain-language summary the patient sees
  - Multi-item prescriptions with downloadable PDF (Lumen-branded)
  - Lab orders (with later result entry)
  - Referrals to other doctors (incoming/outgoing queue)
- Patient context strip: DOB / blood type / **allergies in red** / chronic conditions

### `ROLE_NURSE` (admin-approved)
- Read-only view of appointments assigned to them

### `ROLE_ADMIN`
- Approve / reject pending doctor & nurse registrations
- Approve / reject doctor time-off requests
- Manage all user accounts (create, edit, disable, change password, delete)
- Manage **departments / specialties** (M:N with doctors)
- Manage clinic-wide **holidays** (closure dates wipe slot generation)
- Publish **announcements** (audience: ALL / STAFF / PATIENTS · tone: INFO / SUCCESS / WARNING)
- Audit log — every state change with timestamp + actor
- KPI overview dashboard

## AI features

Two LLM-powered surfaces backed by **Groq**:

1. **Symptom triage** — patient describes symptoms in the booking modal → AI returns suggested department, urgency (LOW/NORMAL/URGENT), draft chief complaint, and red-flag warnings. Strict prompt rejects gibberish (returns an `__INVALID__` sentinel that renders as a friendly amber notice).
2. **Visit summary** — doctor types terse clinical notes → AI rewrites them as a plain-language summary saved on the medical record and visible to the patient.

Plus a separate **classical ML** service in `lab-ai/`:

3. **Lab report analyzer** — patient or doctor uploads a PDF lab report; a RandomForest classifier (trained on synthetic data) predicts the panel condition (HEALTHY / HYPERLIPIDEMIA / ANEMIA / INFECTION / DIABETES / LIVER_DYSFUNCTION) plus per-value flags and severity, all with class-probability bars in the UI.

## Repository structure

```
.
├── backend/                  Spring Boot API
│   ├── src/main/java/...     entities, services, controllers, security, AI integration
│   └── pom.xml
├── frontend/                 Angular 21 SPA
│   ├── src/app/components/   layout, auth, dashboard, appointments,
│   │                         clinical/{consultation, patient-history, referrals, lab-analyzer},
│   │                         admin/{approvals, audit-log, departments, holidays,
│   │                                announcements, time-off, user-management},
│   │                         doctor/{availability, time-off}, profile/{patient, doctor}
│   ├── src/app/services/     auth, data, profile, availability, holiday, time-off,
│   │                         announcement, audit, inbox, lab-analyzer, ai
│   └── proxy.conf.json       /api → backend, /lab-ai → Flask
├── lab-ai/                   Standalone Flask + scikit-learn service
│   ├── app.py                Flask routes + built-in upload UI
│   ├── setup.py              one-shot bootstrap: data → train → samples
│   ├── reference_ranges.py
│   ├── data/generate.py      synthetic dataset (~4800 rows, 6 classes)
│   ├── model/train.py        RandomForest pipeline
│   ├── model/predict.py      ML + per-value flagging + urgency overrides
│   ├── pdf/samples.py        reportlab → 4 styled lab-report PDFs
│   ├── pdf/parser.py         pdfplumber → {test_id: value}
│   └── templates/index.html  built-in upload UI (works without the Angular app)
├── .github/workflows/        CI: backend + frontend pipelines
├── k8s/                      Kubernetes manifests
├── monitoring/               Prometheus + Grafana starter config
├── docker-compose.yml        local dev stack
├── docker-compose.prod.yml   prod-style stack with Prometheus + Grafana
└── .env.example              copy to .env, fill in secrets
```

## Default demo accounts

All four are seeded into H2 on first backend start. Tap a tile on the login page to sign in instantly.

| Role | Email | Password |
|---|---|---|
| Admin | `admin@example.com` | `admin123` |
| Patient | `john@example.com` | `password123` |
| Doctor | `doctor@example.com` | `password123` |
| Nurse | `nurse@example.com` | `password123` |

The demo doctor is pre-seeded with: Cardiology + General Medicine departments, an availability schedule (Mon–Fri 9–12 / 14–17, 30-min slots), and a profile.
The demo patient is pre-seeded with: DOB 1991-06-18, blood type O+, penicillin allergy, mild seasonal asthma, emergency contact.

## Run all three services (dev)

You'll want **three terminals** open. After the one-time setup below, the order is `backend → frontend → lab-ai`.

### One-time setup

```bash
# 1. Clone + copy env template
cp .env.example .env
# 2. Edit .env — at minimum set APP_AI_GROQ_API_KEY (get one at https://console.groq.com/keys)
```

### Backend (Spring Boot · port 18081)

```bash
cd backend
./mvnw spring-boot:run            # macOS / Linux
mvnw.cmd spring-boot:run          # Windows
```

You'll see this banner once it boots — confirms the demo accounts seeded:

```
====================================================
  Lumen Health · demo accounts ready to log in:
    Admin   admin@example.com   admin123
    Patient john@example.com    password123
    Doctor  doctor@example.com  password123
    Nurse   nurse@example.com   password123
====================================================
```

Useful endpoints:

- Health: `http://localhost:18081/api/actuator/health`
- AI status: `http://localhost:18081/api/ai/status`
- Prometheus metrics: `http://localhost:18081/api/actuator/prometheus`
- H2 console: `http://localhost:18081/api/h2-console` (JDBC: `jdbc:h2:mem:testdb`)

### Frontend (Angular dev server · port 4200)

```bash
cd frontend
npm install
npm start
```

Open `http://localhost:4200`. The dev server proxies:
- `/api/*` → Spring backend on `:18081`
- `/lab-ai/*` → Flask service on `:5001`

If you see CORS errors, make sure `.env` has:

```
APP_CORS_ALLOWED_ORIGINS=http://localhost,http://localhost:4200,http://localhost:8080
```

…and restart the backend.

### Lab-AI (Flask + scikit-learn · port 5001)

```bash
cd lab-ai

# create + activate a venv
python -m venv .venv
.venv\Scripts\activate            # Windows
source .venv/bin/activate         # macOS / Linux

# install deps + bootstrap (synthetic data → train → 4 sample PDFs)
pip install -r requirements.txt
python setup.py                   # ~10s, idempotent

# run
python app.py
```

The Flask service serves both:
- An API at `/api/analyze`, `/api/samples`, `/health`
- A built-in browser UI at `http://127.0.0.1:5001` for testing without the Angular app

Once all three are running, the **Lab analyzer** rail-nav entry inside the Angular dashboard pulls samples and analyses through the dev-server proxy.

## Booking and approval workflows

### Patient signup
- Patients are auto-approved → sign in immediately.
- Doctor / Nurse signups land in a **PENDING** state. The admin queue (`/dashboard/admin/approvals`) shows them with Approve / Reject actions; approval grants the requested clinical role and unlocks login.

### Slot-based booking
- Patients open the booking modal → optional AI symptom helper → pick a doctor → a slot picker shows real open windows (◀ / ▶ day stepper, defaults to tomorrow).
- Slot generation honours: doctor's recurring availability, clinic-wide holidays, and approved doctor time-off.
- The backend rejects past times via `@Future` validation.

### Doctor consultation
- For an appointment they're assigned to, the doctor opens **/dashboard/appointments/{id}/consultation** — one page combining the medical record editor, prescription multi-item modal (with PDF download), lab order modal, referral modal, and the AI patient-summary panel.

## Forgot-password flow

1. Open `/forgot-password`
2. Submit account email
3. Watch backend logs — the reset token is logged (mock email delivery)
4. Open `/reset-password`
5. Paste the token + set the new password

Endpoints: `POST /api/auth/forgot-password`, `POST /api/auth/reset-password`.

## Testing

### Backend

```bash
cd backend
./mvnw test
```

Covers auth service/controller, user controller, appointment controller, security access rules.

### Frontend

```bash
cd frontend
npm run test:ci
```

Covers auth service, login component, route configuration, root shell.

### Lab-AI

```bash
cd lab-ai
.venv/Scripts/activate            # Windows
python -m pytest                  # if you add tests later
```

`setup.py` already prints a hold-out classification report (precision / recall / F1) when you bootstrap.

## API summary

### Auth & accounts

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/auth/register` | public | Self-signup with `requestedRole` (PATIENT auto-approves; others go PENDING) |
| `POST` | `/api/auth/login` | public | Returns JWT on success; 403 + typed message on PENDING / REJECTED |
| `POST` | `/api/auth/forgot-password` | public | Issues a mock reset token |
| `POST` | `/api/auth/reset-password` | public | Consumes the token + sets a new password |
| `GET`  | `/api/users/me` | any | Current user details |

### Admin

| Method | Path | Purpose |
|---|---|---|
| `GET` `/POST` `/PUT` `/DELETE` | `/api/admin/users[/...]` | User CRUD + toggle status + change password |
| `GET` | `/api/admin/pending` | Pending registrations |
| `POST` | `/api/admin/users/{id}/approve` | Grant requested role + enable |
| `POST` | `/api/admin/users/{id}/reject` | Mark REJECTED + disable |
| `GET` | `/api/admin/audit?limit=…` | Recent audit-log entries |
| `GET` | `/api/admin/stats` | KPI counts |

### Clinical structure

| Path | What |
|---|---|
| `/api/departments` | Department CRUD (admin-only mutations, public read) |
| `/api/holidays` | Clinic-wide closure dates |
| `/api/announcements`, `/api/announcements/active` | Banner CRUD + active-feed (audience-targeted) |
| `/api/availability/me`, `/api/availability/doctors/{id}/slots?date=…` | Doctor availability + slot generation |
| `/api/time-off/me`, `/api/time-off?status=…`, `/api/time-off/{id}/approve\|reject` | Doctor PTO + admin approval |
| `/api/doctor-profiles/me`, `/api/doctor-profiles/{userId}` | Doctor profile (departments, specialty, fee, bio…) |
| `/api/patient-profiles/me` | Patient medical profile |

### Appointments + clinical core

| Path | What |
|---|---|
| `/api/appointments`, `/api/appointments/meta` | List / create / update / status / delete |
| `/api/medical-records/by-appointment/{id}` | Per-appointment record (CRU) |
| `/api/medical-records/patient/{userId}` | Patient history (records) |
| `/api/prescriptions/by-appointment/{id}` `/patient/{id}` | List + create |
| `/api/prescriptions/{id}/pdf` | Branded PDF download |
| `/api/prescriptions/{id}/dispense\|cancel` | State transitions |
| `/api/lab-orders/by-appointment/{id}` `/patient/{id}` | Lab order CRUD |
| `/api/lab-orders/{id}/start\|result` | Lab status + upload result |
| `/api/referrals/incoming\|outgoing\|patient/{id}` | Doctor referral queues |
| `/api/referrals` `POST` `/api/referrals/{id}/status/{ACCEPTED\|DECLINED\|COMPLETED}` | Create / decide |

### AI (Groq)

| Path | What |
|---|---|
| `GET` `/api/ai/status` | `{ enabled: bool }` |
| `POST` `/api/ai/triage` | Patient symptoms → department + urgency + draft chief complaint + red flags |
| `POST` `/api/ai/visit-summary` | Doctor's notes → patient-friendly summary (optional `save: true` persists) |

### Notifications

| Path | What |
|---|---|
| `GET` `/api/notifications` | Recent in-app notifications |
| `GET` `/api/notifications/unread-count` | Badge counter |
| `POST` `/api/notifications/mark-all-read` | Clear unread |

### Lab-AI service (Flask · port 5001)

| Method | Path | What |
|---|---|---|
| `GET`  | `/` | Built-in upload UI |
| `GET`  | `/health` | `{ ok, modelLoaded, modelMetrics }` |
| `POST` | `/api/analyze` | multipart `file=<PDF>` → analysis JSON |
| `POST` | `/api/analyze-values` | JSON `{ values: { glucose: 145, … } }` (skip PDF parsing) |
| `GET`  | `/api/samples` | List bundled sample PDFs |
| `GET`  | `/api/samples/{name}` | Download a sample PDF |

The Angular app reaches all of these through `/lab-ai/*` (proxy in `frontend/proxy.conf.json`).

## Docker

### Development stack

```bash
cp .env.example .env
docker compose up --build -d
```

App URL: `http://localhost:8080`. (Lab-AI is **not** in compose yet — run it separately for now.)

### Production-oriented stack

```bash
cp .env.example .env
docker compose -f docker-compose.prod.yml up --build -d
```

Adds Prometheus + Grafana on top of frontend / backend / postgres.

## Kubernetes

```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/database-deployment.yaml
kubectl apply -f k8s/backend-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml
kubectl apply -f k8s/services.yaml
```

Update image names + secret values before targeting a real cluster.

## Monitoring

- Prometheus config: `monitoring/prometheus.yml`
- Grafana datasource: `monitoring/grafana/provisioning/datasources/datasource.yml`
- Sample dashboard: `monitoring/grafana/dashboards/spring-boot-observability.json`
- Default ports: Prometheus `9090`, Grafana `3000`

## CI/CD

`.github/workflows/backend.yml` and `.github/workflows/frontend.yml` install deps with caching, run tests, build artefacts, and build Docker images. Lab-AI does not have CI yet.

## Branching strategy

- `main`: stable
- `dev`: integration
- `feature/*`: short-lived feature branches

## Security & deployment notes

- Set a strong `APP_JWT_SECRET` in `.env`
- Restrict `APP_CORS_ALLOWED_ORIGINS` to your real frontend host(s)
- Rotate `APP_AI_GROQ_API_KEY` if it's been pasted anywhere shared (chat, screenshots, logs)
- Replace the mock password-reset delivery with real email integration
- The lab-AI model is trained on synthetic data — it's an educational demo, not a clinical tool
- `.claude/`, `.cursor/`, `.vscode/`, `.env`, `*.log` are all `.gitignore`d

## License

This project is for educational / portfolio use. No license is granted for clinical deployment.
