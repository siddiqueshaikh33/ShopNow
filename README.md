# 🛒 ShopNow – Modern E‑Commerce Website

<p align="center">
  <img src="./frontend/src/assets/shopnow-logo.png" alt="ShopNow App Logo" width="120" />
</p>

ShopNow is a **full-stack e-commerce web application** currently **under active development (Building in Progress)**. 
It is designed with a modern UI, scalable backend, and secure authentication, providing a smooth shopping experience with cart management, order processing, and payment integration.


---

## ✨ Features

### 👤 User Features

* User registration & login (JWT-based authentication)
* Browse products by category
* Product search and filtering
* Add to cart / update cart items
* Order summary and checkout flow
* Secure online payments (Razorpay integration)
* View order history
* Responsive UI for all devices

### 🛠️ Admin Features

* Admin authentication
* Add / update / delete products
* Manage categories
* View all users
* View and manage orders

### 🔐 Security & Performance

* JWT authentication & authorization
* Secure API communication
* Centralized error handling
* Optimized API calls

---

## 🏗️ Architecture

ShopNow follows a **Modular Microservice Architecture**, designed to ensure scalability, maintainability, and clear separation of responsibilities.

### Architecture Overview

* Each core functionality is implemented as an independent module/service
* Services communicate using **REST APIs**
* Centralized authentication using **JWT**
* Business logic isolated at the service layer
* Database operations managed through JPA/Hibernate

```text
Client (React)
   │
   ├── Auth Service
   ├── Product Service
   ├── Cart Service
   ├── Order Service
   └── Payment Service (Razorpay)
           │
        MySQL Database
```

---

## 🧰 Tech Stack

### 🌐 Frontend

* **React.js** – Component-based UI
* **Tailwind CSS** – Modern utility-first styling
* **Axios** – API communication
* **React Context API** – Global state management
* **React Router DOM** – Routing

### ⚙️ Backend

* **Java (Spring Boot)** – REST API development
* **Spring Security** – Authentication & authorization
* **JWT** – Secure token-based auth
* **JPA / Hibernate** – ORM
* **MySQL** – Relational database

### 💳 Payments

* **Razorpay API** – Secure payment gateway

### 🧪 Tools & Utilities

* **Postman** – API testing
* **Git & GitHub** – Version control
* **Maven** – Dependency management

---

## 🗂️ Project Structure

### 📁 Frontend (`/frontend`)

```bash
frontend/
├── src/
│   ├── components/        # Reusable UI components
│   ├── pages/             # Page-level components
│   ├── context/           # Global state (User, Cart)
│   ├── api/               # Axios instances
│   ├── assets/            # Images & static files
│   ├── routes/            # Protected & public routes
│   └── App.jsx
└── package.json
```

### 📁 Backend (`/backend`)

```bash
backend/
├── controller/            # REST controllers
├── service/               # Business logic
├── repository/            # JPA repositories
├── model/                 # Entity classes
├── security/              # JWT & Spring Security config
├── dto/                   # Data Transfer Objects
├── exception/             # Global exception handling
└── ShopNowApplication.java
```

---

## ⚙️ Local Setup Guide

### 🔽 Prerequisites

* Node.js (v18+ recommended)
* Java JDK 17+
* MySQL Server
* Git

---

### ▶️ Backend Setup

1. Clone the repository

```bash
git clone https://github.com/your-username/shopnow.git
cd backend
```

2. Configure `application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/shopnow
spring.datasource.username=root
spring.datasource.password=your_password

jwt.secret=your_jwt_secret
razorpay.key=your_key
razorpay.secret=your_secret
```

3. Run the backend

```bash
mvn spring-boot:run
```

Backend will start at:

```
http://localhost:8080
```

---

### ▶️ Frontend Setup

1. Navigate to frontend

```bash
cd frontend
```

2. Install dependencies

```bash
npm install
```

3. Start the development server

```bash
npm run dev
```

Frontend will run at:

```
http://localhost:5173
```

---

## 📚 Learnings & Takeaways

* Hands-on experience with **full‑stack development**
* Implemented **JWT authentication & role-based access**
* Integrated **Razorpay payment gateway**
* Improved understanding of **REST APIs**
* Learned state management using **Context API**
* Gained experience in **secure backend design**

---

## 🛠️ Tools & Technologies

| Tool            | Purpose         |
| --------------- | --------------- |
| ⚛️ React        | Frontend UI     |
| 🌱 Spring Boot  | Backend APIs    |
| 🐬 MySQL        | Database        |
| 🔐 JWT          | Authentication  |
| 💳 Razorpay     | Payments        |
| 🎨 Tailwind CSS | Styling         |
| 🧪 Postman      | API Testing     |
| 🧠 GitHub       | Version Control |
