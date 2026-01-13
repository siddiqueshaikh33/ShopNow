# ShopNow

**ShopNow** is a **full-stack e-commerce web application** designed to provide users with a seamless shopping experience. Users can browse products, add items to the cart, register/login, and place orders. The project is currently **under development** and includes both frontend and backend components.

---

## 🚀 Project Status
**Status:** In Progress  
- Core functionalities implemented:  
  - User authentication and authorization (JWT)  
  - Product browsing and cart management  
  - Backend API integration  
- Upcoming features:  
  - Complete order management system  
  - Admin panel for product management  
  - Deployment and CI/CD setup  

---

## 🛠 Technologies Used

### Frontend
- React, JSX  
- React Router for routing  
- Context API for state management  
- Axios for API requests  
- Vite as the build tool  

### Backend
- Java, Spring Boot  
- Maven for dependency management  
- REST APIs for frontend communication  
- MySQL database  
- Razorpay for payment integration  

### Tools & Utilities
- Spring Tools Suite (STS) / Eclipse  
- VS Code  
- Postman for API testing  
- Git & GitHub  

---

## 📂 Project Structure

ShopNow/
├── frontend/ # React frontend application
│ ├── src/ # Components, Pages, Routes, Context
│ ├── public/ # Static assets
│ ├── package.json
│ └── .gitignore
├── backend/ # Spring Boot backend application
│ ├── src/main/java # Controllers, Services, Models
│ ├── src/main/resources # application.properties
│ ├── pom.xml
│ └── .gitignore
├── .gitignore
└── README.md


---

## ⚡ Features

- **User Authentication:** Register and login with JWT tokens  
- **Product Management:** Browse products with images and details  
- **Cart System:** Add, remove, and update items in the cart  
- **Payment Integration:** Razorpay sandbox integration  
- **Frontend-Backend Communication:** Axios calling Spring Boot REST APIs  
- **Database Integration:** MySQL for users, products, and orders  

---

## 🔧 Installation & Setup

### Prerequisites
- Node.js & npm (for frontend)  
- Java JDK 17+ (for backend)  
- Maven (for backend)  
- MySQL database  

---

### 1️⃣ Clone the Repository

```bash
git clone https://github.com/siddiqueshaikh33/ShopNow.git
cd ShopNow
cd backend

Create a .env file
DB_URL=jdbc:mysql://localhost:3306/shopnow
DB_USERNAME=root
DB_PASSWORD=yourpassword

RAZORPAY_KEY=your_razorpay_key
RAZORPAY_SECRET=your_razorpay_secret

Update application.properties to use environment variables:
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

razorpay.key=${RAZORPAY_KEY}
razorpay.secret=${RAZORPAY_SECRET}

Run backend:
./mvnw spring-boot:run

