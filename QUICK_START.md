# 🚀 Quick Start Guide

Get the application running in 5 minutes!

## Prerequisites Check

```bash
# Check Java version (need 21+)
java -version

# Check Node/npm version (need Node 18+, npm 11+)
node --version
npm --version

# Check Maven version
mvn --version
```

## 1️⃣ Start Backend (Terminal 1)

```bash
cd D:\Hamza\backend

# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run
```

**Wait for**: `Started DemoApplication in X seconds`

Backend running at: `http://localhost:8080`

## 2️⃣ Start Frontend (Terminal 2)

```bash
cd D:\Hamza\frontend

# Install dependencies (first time only)
npm install

# Start development server
npm start
```

**Wait for**: `✔ Open http://localhost:4200 in your browser`

Frontend running at: `http://localhost:4200`

## 3️⃣ Test the Application

### Step 1: Register
1. Open `http://localhost:4200`
2. Click "Register here" link
3. Fill in the form:
   - First Name: `John`
   - Last Name: `Doe`
   - Email: `john@example.com`
   - Password: `password123`
   - Confirm: `password123`
4. Click "Register"

### Step 2: Logged In! 🎉
You'll see the dashboard with:
- Welcome message
- User info cards
- Quick action buttons

### Step 3: Logout & Login
1. Click "Logout" button (top-right)
2. Use credentials to login again:
   - Email: `john@example.com`
   - Password: `password123`

### Step 4: Admin Features (Optional)

To test admin user management:

1. Open `http://localhost:8080/h2-console`
2. Execute this SQL:

```sql
-- Create admin user
INSERT INTO users (email, password, first_name, last_name, enabled) 
VALUES ('admin@example.com', '$2a$10$K6eoHi8qW7v8Nuz9JcJ2.OqTq6r1KYhZKHDT0/1NwT2p/yDHWZsNm', 'Admin', 'User', true);

-- Assign ADMIN role
INSERT INTO user_roles (user_id, role_id) VALUES (2, 2);
```

3. Logout from the current user
4. Login as admin:
   - Email: `admin@example.com`
   - Password: `password123`
5. Click "Users" in sidebar to see user management

## 🐛 Troubleshooting

### Backend won't start
```bash
# Check if port 8080 is free
# Or change port in application.properties: server.port=8081
```

### Frontend shows blank page
```bash
# Clear cache
# Press Ctrl+Shift+Delete in browser, clear cache

# Check console (F12) for errors
# Ensure backend is running on http://localhost:8080
```

### Can't login after registration
```bash
# Check H2 console for user data:
# http://localhost:8080/h2-console
# Query: SELECT * FROM users;

# Verify password was hashed in database
```

### CORS errors in browser console
```
Access to XMLHttpRequest blocked by CORS policy
```

- Backend CORS is configured for `http://localhost:4200`
- If using different port, update in `backend/src/main/java/com/example/demo/config/SecurityConfig.java`

## 📁 File Structure

```
D:\Hamza\
├── backend/
│   ├── src/main/java/com/example/demo/    ← Backend code
│   ├── src/main/resources/
│   │   └── application.properties          ← Config
│   ├── pom.xml                             ← Dependencies
│   └── target/
│       └── demo-0.0.1-SNAPSHOT.jar         ← Built JAR
│
├── frontend/
│   ├── src/app/                            ← Frontend code
│   ├── package.json                        ← Dependencies
│   ├── angular.json                        ← Angular config
│   ├── tailwind.config.ts                  ← Tailwind config
│   └── dist/                               ← Built files
│
├── SETUP_GUIDE.md                          ← Detailed guide
├── IMPLEMENTATION_SUMMARY.md               ← What was built
└── QUICK_START.md                          ← This file
```

## 🔐 Authentication Flow

```
1. User enters credentials
   ↓
2. Frontend POSTs to /api/auth/login
   ↓
3. Backend validates with BCrypt
   ↓
4. Backend returns JWT token
   ↓
5. Frontend stores token in localStorage
   ↓
6. AuthInterceptor adds token to all requests: Authorization: Bearer <token>
   ↓
7. Backend JwtAuthenticationFilter validates token on each request
   ↓
8. User can access protected resources
```

## 🔐 JWT Token Details

- **Where stored**: Browser's `localStorage` (key: `auth_token`)
- **Expiration**: 24 hours
- **Secret**: Set in `application.properties` (change for production!)
- **Algorithm**: HS256

## 🎯 Key URLs

| Service | URL | Purpose |
|---------|-----|---------|
| Frontend | http://localhost:4200 | Web UI |
| Backend API | http://localhost:8080/api | REST API |
| H2 Console | http://localhost:8080/h2-console | Database UI |
| Swagger (future) | http://localhost:8080/swagger-ui.html | API docs |

## 📊 Database

**Type**: H2 (in-memory)
**Console**: http://localhost:8080/h2-console
**Connection**: `jdbc:h2:mem:testdb`
**User**: `sa`
**Password**: (empty)

**Tables**:
- `users` - User accounts
- `roles` - User roles (USER, ADMIN)
- `user_roles` - User-role mappings

## 🧪 API Testing with cURL

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName":"John",
    "lastName":"Doe",
    "email":"john@example.com",
    "password":"password123",
    "confirmPassword":"password123"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email":"john@example.com",
    "password":"password123"
  }'

# Get current user (replace TOKEN with actual JWT)
curl -H "Authorization: Bearer TOKEN" \
  http://localhost:8080/api/users/me

# Get all users (ADMIN only)
curl -H "Authorization: Bearer TOKEN" \
  http://localhost:8080/api/users
```

## 🚀 Next Steps

1. **Explore the code**: All components are well-commented
2. **Read full guides**: `SETUP_GUIDE.md` and `IMPLEMENTATION_SUMMARY.md`
3. **Modify and extend**: Change colors, add features, etc.
4. **Deploy**: Follow production deployment steps in SETUP_GUIDE.md

## 💡 Tips

- **Change Port**: Edit `application.properties` (backend) or use `npm start -- --port 5000` (frontend)
- **Database**: Data is lost when backend stops (H2 in-memory)
- **Styles**: All CSS uses Tailwind - modify `tailwind.config.ts`
- **API**: All endpoints documented in SETUP_GUIDE.md
- **Testing**: Run `mvn test` in backend directory

## 🎓 Learning Path

1. Start frontend: **http://localhost:4200**
2. Navigate to Register, understand the flow
3. Check browser DevTools Network tab to see API calls
4. Login and explore the dashboard
5. Check H2 console to see database
6. Read source code in order:
   - Backend: `service/AuthService.java`
   - Backend: `controller/AuthController.java`
   - Frontend: `services/auth.service.ts`
   - Frontend: `components/auth/login/`

## ✨ Features to Try

- ✅ Register with different emails
- ✅ Try invalid inputs (validation in action)
- ✅ Logout and login again
- ✅ Open H2 console while logged in
- ✅ Check browser Storage (DevTools) for JWT token
- ✅ Try accessing `/dashboard/admin/users` as regular user (403 error)
- ✅ Try modifying requests in browser DevTools

## 📞 Support

Having issues? Check:
1. **SETUP_GUIDE.md** - Detailed troubleshooting section
2. **IMPLEMENTATION_SUMMARY.md** - Architecture and design details
3. **Source code comments** - Each file has documentation
4. **Console errors** - Browser F12 and backend logs

---

**Status**: ✅ Ready to run!
**Time to first app start**: ~5 minutes
**Time to full working system**: ~10 minutes (including npm install)

Happy coding! 🎉

