# Full-Stack Application Setup & Testing Guide

This document provides comprehensive instructions to set up, run, and test the complete full-stack application.

## Overview

The application consists of two main components:

- **Backend**: Spring Boot 4.0.4 with JWT authentication, Spring Security, and REST API
- **Frontend**: Angular 21 with Tailwind CSS, reactive forms, and role-based UI

## Prerequisites

- Java 21+
- Node.js 18+ and npm 11+
- Maven 3.6+
- Git (optional)

## Backend Setup & Execution

### Step 1: Build the Backend

```bash
cd D:\Hamza\backend
mvn clean package -DskipTests
```

The build will create a JAR file at: `target/demo-0.0.1-SNAPSHOT.jar`

### Step 2: Run the Backend Server

```bash
mvn spring-boot:run
```

Or directly run the JAR:

```bash
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

The backend will start on: **http://localhost:8080**

#### Backend API Endpoints

- **POST** `/api/auth/register` - Register a new user
- **POST** `/api/auth/login` - User login (returns JWT token)
- **GET** `/api/auth/validate` - Validate JWT token
- **GET** `/api/users/me` - Get current user profile
- **GET** `/api/users` - Get all users (ADMIN only)
- **GET** `/api/users/{id}` - Get user by ID (ADMIN only)
- **PUT** `/api/users/{id}` - Update user (ADMIN only)
- **DELETE** `/api/users/{id}` - Delete user (ADMIN only)

#### H2 Database Console

Access the H2 console at: **http://localhost:8080/h2-console**

- URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (leave empty)

## Frontend Setup & Execution

### Step 1: Install Dependencies

```bash
cd D:\Hamza\frontend
npm install
```

### Step 2: Run Development Server

```bash
npm start
```

The frontend will start on: **http://localhost:4200**

### Step 3: Build for Production

```bash
npm run build
```

## Testing the Application

### 1. Register a New User

1. Navigate to **http://localhost:4200/register**
2. Fill in the registration form:
   - First Name: John
   - Last Name: Doe
   - Email: john@example.com
   - Password: password123
   - Confirm Password: password123
3. Click "Register"
4. You'll be redirected to the dashboard

### 2. Login

1. Navigate to **http://localhost:4200/login**
2. Enter credentials:
   - Email: john@example.com
   - Password: password123
3. Click "Sign In"

### 3. Access Dashboard

After login, you'll see:
- User profile information
- Welcome message with first/last name
- Assigned roles
- Quick action buttons

### 4. Admin Features (User Management)

To access admin features, you need an ADMIN role. Here's how to create an admin user:

#### Option A: Using H2 Console

1. Open **http://localhost:8080/h2-console**
2. Run these SQL commands:

```sql
-- Create admin user
INSERT INTO users (email, password, first_name, last_name, enabled) 
VALUES ('admin@example.com', '$2a$10$K6eoHi8qW7v8Nuz9JcJ2.OqTq6r1KYhZKHDT0/1NwT2p/yDHWZsNm', 'Admin', 'User', true);

-- Get the IDs
SELECT id FROM users;
SELECT id FROM roles WHERE name = 'ADMIN';

-- Assign ADMIN role (replace 1 and 1 with actual IDs if different)
INSERT INTO user_roles (user_id, role_id) VALUES (1, 2);
```

Note: The password hash is for "password123" (BCrypt encrypted)

#### Option B: Register User, then Set Admin Role

1. Register a user normally
2. Use H2 console to assign the ADMIN role to that user

#### Access User Management

1. Login with an admin account
2. In the sidebar, you'll see "Users" under "Admin" section
3. Click "Users" to access the user management page
4. You can:
   - View all users in a table
   - Edit user details (first name, last name, enabled status)
   - Delete users
   - Add new users (create button)

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true
);
```

### Roles Table
```sql
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) UNIQUE NOT NULL,
    description VARCHAR(255)
);
```

### User_Roles Junction Table
```sql
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);
```

## JWT Token Details

- **Secret Key**: Configured in `application.properties` (change in production!)
- **Expiration**: 24 hours (86400000 milliseconds)
- **Format**: Bearer token in Authorization header
- **Example**: `Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...`

## Key Features Implemented

### Backend Features
✅ Spring Security with JWT authentication
✅ Role-based access control (ROLE_USER, ROLE_ADMIN)
✅ DTO pattern for request/response
✅ BCrypt password encryption
✅ JPA/Hibernate for database operations
✅ Global exception handling
✅ CORS configuration
✅ Unit tests with Mockito
✅ H2 in-memory database

### Frontend Features
✅ Angular reactive forms
✅ Tailwind CSS styling
✅ Auth interceptor for JWT tokens
✅ Route guards (AuthGuard, AdminGuard)
✅ Error interceptor with notifications
✅ Responsive layout with sidebar navigation
✅ User management CRUD operations
✅ Real-time form validation
✅ Toast notifications

## Troubleshooting

### Backend Issues

**Issue**: Port 8080 already in use
```bash
# Change port in application.properties
server.port=8081
```

**Issue**: JWT secret key error
- Ensure `app.jwt.secret` is set in `application.properties`
- Must be at least 256 bits (32 characters) for HS256

### Frontend Issues

**Issue**: CORS errors
- Backend is configured to allow requests from `http://localhost:4200`
- If running on different port, update SecurityConfig

**Issue**: Blank page after login
- Clear browser cache and localStorage
- Check console for errors
- Verify backend is running on `http://localhost:8080`

## File Structure

### Backend
```
backend/
├── src/main/java/com/example/demo/
│   ├── config/          # Security & DB config
│   ├── controller/       # REST endpoints
│   ├── dto/             # Request/Response objects
│   ├── entity/          # JPA entities
│   ├── exception/       # Exception handling
│   ├── mapper/          # DTO mapping
│   ├── repository/      # Data access
│   ├── security/        # JWT & Security filters
│   └── service/         # Business logic
├── src/test/java/       # Unit tests
└── pom.xml              # Maven dependencies
```

### Frontend
```
frontend/src/
├── app/
│   ├── components/      # UI components
│   │   ├── auth/       # Login/Register
│   │   ├── dashboard/  # Dashboard page
│   │   ├── admin/      # Admin pages
│   │   └── layout/     # Main layout
│   ├── guards/         # Route guards
│   ├── interceptors/   # HTTP interceptors
│   ├── services/       # Services
│   ├── app.config.ts   # App configuration
│   └── app.routes.ts   # Routing
├── styles.css          # Global styles
└── index.html          # Entry point
```

## Next Steps & Enhancements

1. **Production Deployment**
   - Move to PostgreSQL from H2
   - Set secure JWT secret via environment variables
   - Implement refresh token logic
   - Add HTTPS/SSL certificates

2. **Additional Features**
   - Email verification on registration
   - Password reset functionality
   - Two-factor authentication (2FA)
   - API documentation (Swagger/OpenAPI)
   - Rate limiting

3. **Testing**
   - Add integration tests
   - Increase unit test coverage
   - Add E2E tests with Cypress
   - Load testing

4. **Security Enhancements**
   - Implement refresh tokens
   - Add CSRF protection if needed
   - Rate limiting on auth endpoints
   - Account lockout after failed attempts
   - Audit logging

## Support & Documentation

For more information:
- [Spring Boot 4.0 Documentation](https://spring.io/projects/spring-boot)
- [Angular 21 Documentation](https://angular.io/docs)
- [Tailwind CSS Documentation](https://tailwindcss.com/docs)
- [JWT Best Practices](https://tools.ietf.org/html/rfc7519)

---

**Last Updated**: March 25, 2026
**Version**: 1.0.0

