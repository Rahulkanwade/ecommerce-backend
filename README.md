# 🛒 E-Commerce Backend

A production-ready e-commerce REST API built with Spring Boot 4.x.

## Tech Stack
- Java 17 + Spring Boot 4.x
- Spring Security + JWT Authentication
- Spring Data JPA + PostgreSQL
- PayPal Payment Integration
- Maven

## Features
- User Registration & Login with JWT
- Role-based access (USER, ADMIN)
- Product CRUD (Admin only for write)
- Cart Management with stock validation
- Order Management
- PayPal Payment Integration
- Global Exception Handling
- Unit Tests (16 tests)

## API Endpoints

| Method | Endpoint | Access |
|--------|----------|--------|
| POST | /api/auth/register | Public |
| POST | /api/auth/login | Public |
| GET | /api/products | Public |
| POST | /api/products | ADMIN |
| GET | /api/cart | USER |
| POST | /api/orders | USER |
| POST | /api/payments/create | USER |

## Setup

1. Clone the repository
2. Create PostgreSQL database: `ecommerce_db`
3. Update `application.properties` with your credentials
4. Run: `mvnw spring-boot:run`

## Running Tests
mvnw test