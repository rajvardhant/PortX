# 🚚 Dynamic Route Optimization System

A **beginner-friendly academic logistics management web application** built with Java 21, Spring Boot 3, MySQL, and Thymeleaf.

---

## 📋 Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.2.5 |
| Security | Spring Security + JWT (JJWT 0.11.5) |
| ORM | Spring Data JPA + Hibernate |
| Database | MySQL 8 |
| Frontend | Thymeleaf + Bootstrap 5 + Chart.js + Leaflet.js |
| Build | Apache Maven |
| Testing | JUnit 5 + Mockito |
| API | OpenRouteService (Routes) + OpenStreetMap (Maps) |

---

## 🚀 Getting Started (IntelliJ IDEA)

### Prerequisites
- JDK 21 (set in Project SDK)
- MySQL 8.x running locally
- IntelliJ IDEA (Ultimate or Community with Spring plugin)
- Maven (bundled with IntelliJ)

### Step 1 — Database Setup
Open MySQL Workbench / MySQL CLI and run:
```sql
CREATE DATABASE routedb;
```
> The app will auto-create all tables on first run.

### Step 2 — Configure Database Password
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD   ← Change this
```

### Step 3 — Open in IntelliJ
1. Open IntelliJ IDEA
2. **File → Open** → Select `d:\PortX\dynamic-route-system`
3. IntelliJ will detect the Maven project and import it automatically
4. Wait for Maven to download all dependencies (first run takes ~2 min)
5. **Enable Annotation Processing**: File → Settings → Build → Compiler → Annotation Processors → ✅ Enable

### Step 4 — Run the Application
1. Find `RouteSystemApplication.java` in `src/main/java/com/portx/routesystem/`
2. Right-click → **Run 'RouteSystemApplication'**
3. Open browser: [http://localhost:8080](http://localhost:8080)

---

## 🔑 Default Login Credentials

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Dispatcher | `dispatcher` | `admin123` |
| Driver | `driver1` | `admin123` |

---

## 📁 Project Structure

```
dynamic-route-system/
├── pom.xml                          ← Maven configuration
├── src/main/java/com/portx/routesystem/
│   ├── RouteSystemApplication.java  ← Main entry point
│   ├── config/
│   │   ├── AppConfig.java           ← RestTemplate bean
│   │   └── SecurityConfig.java      ← Spring Security config
│   ├── controller/
│   │   ├── AuthController.java      ← REST: /api/auth/**
│   │   ├── DriverController.java    ← REST: /api/drivers/**
│   │   ├── VehicleController.java   ← REST: /api/vehicles/**
│   │   ├── RouteController.java     ← REST: /api/routes/**
│   │   ├── DeliveryController.java  ← REST: /api/deliveries/**
│   │   ├── InvoiceController.java   ← REST: /api/invoices/**
│   │   └── WebController.java       ← Thymeleaf page routing
│   ├── dto/                         ← Request/Response objects
│   ├── entity/                      ← JPA Entities + Enums
│   ├── exception/                   ← Custom exceptions + Handler
│   ├── repository/                  ← Spring Data JPA repos
│   ├── security/                    ← JWT filter, util, UserDetails
│   └── service/                     ← Business logic
│
├── src/main/resources/
│   ├── application.properties       ← App configuration
│   ├── data.sql                     ← Seed data (auto-runs)
│   ├── templates/                   ← Thymeleaf HTML pages
│   │   ├── fragments/common.html    ← Sidebar + navbar fragments
│   │   ├── index.html               ← Landing page
│   │   ├── auth/                    ← Login, Register
│   │   ├── admin/                   ← Dashboard, Drivers, Vehicles, Routes
│   │   ├── dispatcher/              ← Deliveries management
│   │   ├── driver/                  ← Driver portal
│   │   └── invoice/                 ← Invoice list + detail
│   └── static/
│       ├── css/style.css            ← Main stylesheet
│       ├── css/print.css            ← Print-only styles
│       └── js/                      ← JavaScript modules
│
└── src/test/java/                   ← JUnit + Mockito tests
```

---

## 🌐 REST API Endpoints

### Authentication
```
POST /api/auth/login     → Login (returns JWT token)
POST /api/auth/register  → Register (ADMIN only)
```

### Drivers
```
GET    /api/drivers       → List all drivers
POST   /api/drivers       → Create driver (ADMIN)
PUT    /api/drivers/{id}  → Update driver (ADMIN)
DELETE /api/drivers/{id}  → Delete driver (ADMIN)
```

### Vehicles
```
GET    /api/vehicles       → List all vehicles
POST   /api/vehicles       → Create vehicle (ADMIN)
PUT    /api/vehicles/{id}  → Update vehicle (ADMIN)
DELETE /api/vehicles/{id}  → Delete vehicle (ADMIN)
```

### Routes
```
POST /api/routes/generate  → Generate route via ORS API
GET  /api/routes            → List all routes
GET  /api/routes/{id}       → Get route by ID
```

### Deliveries
```
GET   /api/deliveries              → List all deliveries
POST  /api/deliveries              → Create delivery (auto-generates invoice)
PUT   /api/deliveries/{id}         → Update delivery
PATCH /api/deliveries/{id}/status  → Update status only
GET   /api/deliveries/driver/{id}  → Deliveries for a driver
```

### Invoices
```
GET   /api/invoices              → List all invoices
GET   /api/invoices/{id}         → Get invoice by ID
PATCH /api/invoices/{id}/status  → Update invoice status
```

---

## 💡 Key Features

- ✅ **JWT Authentication** — Secure login with Bearer token
- ✅ **Role-Based Access** — Admin, Dispatcher, Driver roles
- ✅ **Route Planning** — Real routes via OpenRouteService API
- ✅ **Map Visualization** — Leaflet.js + OpenStreetMap
- ✅ **Auto Invoice** — Generated automatically on delivery creation
- ✅ **Print Invoice** — Browser print dialog with clean CSS
- ✅ **Dashboard Charts** — Chart.js delivery & invoice charts
- ✅ **Responsive UI** — Bootstrap 5, mobile-friendly
- ✅ **Seed Data** — Ready to demo on first run

---

## 🧪 Running Tests

In IntelliJ: Right-click `src/test` → **Run All Tests**

Or via Maven terminal:
```bash
mvn test
```

Test coverage:
- `AuthServiceTest` — Login, register, duplicate user
- `DriverServiceTest` — CRUD, not found exception
- `VehicleServiceTest` — CRUD operations
- `DeliveryServiceTest` — Create, status update
- `InvoiceServiceTest` — List, get, status update

---

## 🗄️ Database Schema

| Table | Key Fields |
|---|---|
| `users` | userId, username, password (BCrypt), email, role |
| `drivers` | driverId, name, phone, licenseNumber, status, vehicleId FK |
| `vehicles` | vehicleId, registrationNumber, vehicleType, capacity, status |
| `routes` | routeId, startLocation, endLocation, distance, estimatedTime, polyline |
| `deliveries` | deliveryId, customerName, status, driverId FK, vehicleId FK, routeId FK |
| `invoices` | invoiceId, deliveryId FK, amount, status, invoiceDate |

---

## ⚠️ Troubleshooting

| Problem | Solution |
|---|---|
| `Communications link failure` | MySQL not running — start MySQL service |
| `Access denied for user 'root'` | Check password in `application.properties` |
| `Port 8080 already in use` | Change `server.port=8081` in properties |
| `Cannot find symbol` Lombok error | Enable Annotation Processing in IntelliJ settings |
| Route generation fails | Check ORS API key in `application.properties` |
| `Whitelabel Error Page` on login | Make sure you're accessing `http://localhost:8080/login` |
