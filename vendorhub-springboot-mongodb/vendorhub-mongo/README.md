# 🔥 VendorHub Food Delivery — Spring Boot + MongoDB

Complete REST API backend using **Spring Boot 3.2** + **MongoDB**.

---

## 📁 Project Structure

```
vendorhub-backend/
├── pom.xml
└── src/main/java/com/vendorhub/
    ├── VendorHubApplication.java        ← Main class
    ├── document/                        ← MongoDB documents
    │   ├── User.java
    │   ├── Restaurant.java
    │   ├── MenuItem.java
    │   ├── Order.java                   ← Embedded items + timeline
    │   ├── WalletTransaction.java
    │   ├── Review.java
    │   ├── Message.java
    │   └── Notification.java
    ├── repository/                      ← MongoRepository interfaces
    ├── service/                         ← Business logic
    │   ├── AuthService.java
    │   ├── OrderService.java            ← Full order lifecycle
    │   └── WalletService.java           ← Debit/Credit/Notify
    ├── controller/                      ← REST endpoints
    │   ├── AuthController.java
    │   ├── RestaurantController.java
    │   ├── MenuController.java
    │   ├── OrderController.java
    │   ├── WalletController.java
    │   ├── MessageController.java
    │   ├── NotificationController.java
    │   ├── DeliveryController.java
    │   └── UserController.java
    ├── security/
    │   ├── JwtUtil.java
    │   └── JwtAuthFilter.java
    ├── config/
    │   ├── SecurityConfig.java
    │   └── DataSeeder.java              ← Auto seeds on startup
    └── exception/
        ├── ApiException.java
        └── GlobalExceptionHandler.java
```

---

## ⚡ Quick Start

### Step 1 — Install MongoDB
```
Windows: Download from https://www.mongodb.com/try/download/community
         Install and start the MongoDB service
Mac:     brew tap mongodb/brew && brew install mongodb-community && brew services start mongodb-community
```

### Step 2 — Build and Run
```bash
# Open the project in IntelliJ IDEA or any IDE
# OR run from terminal:

mvn spring-boot:run
```

### Step 3 — Done!
Server starts at: **http://localhost:8080**

MongoDB database `vendorhub` is created automatically.
Sample data is loaded automatically on first startup.

---

## 🔑 Test Accounts (auto-created on startup)

| Role     | Email                        | Password    |
|----------|------------------------------|-------------|
| Customer | rahul@example.com            | password123 |
| Customer | priya@example.com            | password123 |
| Vendor   | amit@spicegarden.com         | vendor123   |
| Vendor   | sara@burgerbarn.com          | vendor123   |
| Delivery | ravi@deliver.com             | rider123    |

---

## 📡 API Endpoints

### Base URL: `http://localhost:8080/api`

### Authentication
All protected routes need:
```
Authorization: Bearer <accessToken>
```

---

### 🔐 AUTH `/api/auth`

| Method | Endpoint     | Auth | Description                   |
|--------|-------------|------|-------------------------------|
| POST   | /register   | ❌   | Register (customer/vendor/delivery) |
| POST   | /login      | ❌   | Login → get JWT tokens        |
| GET    | /me         | ✅   | Get current user              |
| PATCH  | /profile    | ✅   | Update name/phone/avatar      |

**Register Body:**
```json
{
  "name": "Rahul Sharma",
  "email": "test@example.com",
  "phone": "9876543210",
  "password": "pass123",
  "role": "CUSTOMER"
}
```
For VENDOR also add: `"restName": "My Restaurant", "cuisine": "North Indian"`
For DELIVERY also add: `"vehicle": "Scooter"`

**Login Response:**
```json
{
  "success": true,
  "data": {
    "id": "64abc...",
    "name": "Rahul Sharma",
    "role": "CUSTOMER",
    "wallet": 500.00,
    "accessToken": "eyJ...",
    "refreshToken": "eyJ..."
  }
}
```

---

### 🏪 RESTAURANTS `/api/restaurants`

| Method | Endpoint              | Auth   | Description              |
|--------|-----------------------|--------|--------------------------|
| GET    | /                     | ❌     | List all (filter: cuisine, search, open) |
| GET    | /:id                  | ❌     | Details + menu + reviews |
| GET    | /me/mine              | Vendor | My restaurant            |
| PATCH  | /:id                  | Vendor | Update restaurant        |
| PATCH  | /:id/toggle-open      | Vendor | Open/Close toggle        |
| GET    | /:id/stats            | Vendor | Revenue, orders, ratings |

---

### 🍴 MENU `/api/menu`

| Method | Endpoint       | Auth   | Description          |
|--------|---------------|--------|----------------------|
| GET    | /?restId=xxx  | ❌     | Get menu items       |
| GET    | /:id          | ❌     | Single item          |
| POST   | /             | Vendor | Add item             |
| PATCH  | /:id          | Vendor | Edit item            |
| PATCH  | /:id/toggle   | Vendor | Show/Hide item       |
| DELETE | /:id          | Vendor | Delete item          |

---

### 📦 ORDERS `/api/orders`

| Method | Endpoint          | Auth     | Description                    |
|--------|------------------|----------|--------------------------------|
| POST   | /                 | Customer | Place order                    |
| GET    | /                 | Any      | My orders (role-aware)         |
| GET    | /available        | Delivery | Orders ready for pickup        |
| GET    | /:id              | Any      | Order details + timeline       |
| PATCH  | /:id/status       | Any      | Update status                  |
| POST   | /:id/review       | Customer | Submit review                  |

**Order Status Flow:**
```
Customer → PENDING
Vendor   → CONFIRMED → PREPARING → READY
Delivery → PICKED → DELIVERED
Anyone   → CANCELLED (auto refund if cancelled)
```

**Place Order Body:**
```json
{
  "restId": "64abc...",
  "items": [
    { "itemId": "64def...", "quantity": 2 },
    { "itemId": "64ghi...", "quantity": 1 }
  ],
  "address": "14/2 Safdarjung Enclave, New Delhi",
  "instructions": "Extra spicy please!"
}
```

**Update Status Body:**
```json
{ "status": "CONFIRMED", "note": "Order confirmed!" }
```

---

### 💳 WALLET `/api/wallet`

| Method | Endpoint        | Auth | Description                  |
|--------|----------------|------|------------------------------|
| GET    | /               | ✅   | Balance + transactions       |
| POST   | /topup          | ✅   | Add money                    |
| GET    | /transactions   | ✅   | Transaction history          |
| GET    | /summary        | ✅   | Total credited/debited       |

---

### 💬 MESSAGES `/api/messages`

| Method | Endpoint             | Auth | Description              |
|--------|---------------------|------|--------------------------|
| GET    | /conversations       | ✅   | All chats                |
| GET    | /:partnerId          | ✅   | Chat with one user       |
| POST   | /                    | ✅   | Send message             |
| GET    | /unread/count        | ✅   | Unread count             |

---

### 🔔 NOTIFICATIONS `/api/notifications`

| Method | Endpoint         | Auth | Description            |
|--------|-----------------|------|------------------------|
| GET    | /                | ✅   | All notifications      |
| PATCH  | /:id/read        | ✅   | Mark one read          |
| PATCH  | /read/all        | ✅   | Mark all read          |
| DELETE | /:id             | ✅   | Delete one             |
| DELETE | /clear/all       | ✅   | Delete all             |

---

### 🛵 DELIVERY `/api/delivery`

| Method | Endpoint     | Auth     | Description           |
|--------|-------------|----------|-----------------------|
| GET    | /available   | Delivery | Open pickups          |
| GET    | /active      | Delivery | Current delivery      |
| GET    | /history     | Delivery | All past deliveries   |
| GET    | /earnings    | Delivery | Wallet + breakdown    |
| GET    | /stats       | Delivery | Dashboard numbers     |

---

### 👤 USERS `/api/users`

| Method | Endpoint    | Auth | Description             |
|--------|------------|------|-------------------------|
| GET    | /profile    | ✅   | Full profile            |
| PATCH  | /profile    | ✅   | Update profile          |
| GET    | /reviews    | ✅   | My reviews              |
| GET    | /addresses  | ✅   | Recent delivery addrs   |

---

## 💡 Automatic Money Flow

| Event            | Action                                   |
|------------------|------------------------------------------|
| Customer signup  | ₹500 wallet bonus auto-credited          |
| Order placed     | Amount deducted from customer wallet     |
| Order delivered  | ₹40 credited to delivery partner         |
| Order delivered  | 85% of total credited to vendor          |
| Order cancelled  | Full refund to customer                  |

---

## 🔧 MongoDB vs H2 Comparison

| Feature          | MongoDB                    | H2 (JPA)          |
|------------------|---------------------------|-------------------|
| Schema           | Flexible (schemaless)     | Fixed tables      |
| Order items      | Embedded in Order document| Separate table    |
| Timeline         | Embedded array            | Separate table    |
| Setup            | Install MongoDB separately| Zero setup        |
| IDs              | String (ObjectId)         | Long (auto-inc)   |
| Best for         | Flexible/scalable apps    | Simple apps       |
