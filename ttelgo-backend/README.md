# TTelGo Backend

TTelGo is a next-generation eSIM platform designed to enable seamless global connectivity. This backend service provides the core API for managing users, eSIMs, orders, payments, and integrations with eSIMGo and Stripe.

## 🚀 Features

- **User Management** - User registration, authentication, and profile management
- **OTP Authentication** - Secure OTP-based login system with JWT tokens
- **eSIM Management** - eSIM provisioning, QR code generation, and usage tracking
- **Order Processing** - Complete order lifecycle management
- **Payment Integration** - Stripe payment gateway integration
- **KYC Verification** - Identity verification workflow
- **Plan Management** - Country and data plan management via eSIMGo API
- **Admin Dashboard** - Administrative operations and analytics
- **Notifications** - Email, SMS, and push notification support

## 🏗️ Architecture

This project follows **Clean Architecture** principles with clear separation of concerns:

```
com.tiktel.ttelgo
├── api/          # REST Controllers, DTOs, Mappers
├── application/   # Business logic, Use cases, Ports
├── domain/        # Domain entities, Enums
└── infrastructure/# Repositories, Adapters, External integrations
```

### Module Structure

Each feature module follows the same structure:
- **api/** - REST endpoints, request/response DTOs, API mappers
- **application/** - Service layer, use case orchestration, port interfaces
- **domain/** - Domain entities, value objects, enums
- **infrastructure/** - JPA repositories, external service adapters, configuration

## 🛠️ Tech Stack

- **Framework:** Spring Boot 3.5.8
- **Language:** Java 17
- **Database:** PostgreSQL 16+
- **ORM:** JPA/Hibernate
- **Migration:** Flyway
- **Security:** Spring Security + JWT
- **Documentation:** SpringDoc OpenAPI (Swagger)
- **Build Tool:** Maven
- **Cloud:** AWS (ready for deployment)

## 📋 Prerequisites

Before running the application, ensure you have:

- **Java 17** or higher
- **Maven 3.6+**
- **PostgreSQL 16+** installed and running
- **Git** for version control

## ⚙️ Configuration

### Database Setup

1. Create PostgreSQL database:
   ```sql
   CREATE DATABASE ttelgo_dev;
   ```

2. Update `src/main/resources/application-dev.yml` with your database credentials:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/ttelgo_dev
       username: postgres
       password: your_password
   ```

### Environment Variables

You can also use environment variables:
```bash
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
```

### Stripe Checkout (local)
- `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` (backend), `FRONTEND_URL`
- See `docs/stripe-local.md` for end-to-end local test steps (Checkout + webhooks).

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone <your-repo-url>
cd ttelgo-backend
```

### 2. Build the Project

```bash
mvn clean install
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Access API Documentation

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **API Docs:** http://localhost:8080/api-docs
- **Health Check:** http://localhost:8080/actuator/health

## 📁 Project Structure

```
ttelgo-backend/
├── src/
│   ├── main/
│   │   ├── java/com/tiktel/ttelgo/
│   │   │   ├── common/          # Shared utilities, exceptions, DTOs
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── security/        # Security configuration, JWT
│   │   │   ├── user/            # User management module
│   │   │   ├── auth/            # Authentication module (OTP, JWT)
│   │   │   ├── plan/            # Plan and country management
│   │   │   ├── order/           # Order management
│   │   │   ├── payment/         # Payment processing (Stripe)
│   │   │   ├── esim/            # eSIM provisioning and management
│   │   │   ├── kyc/             # KYC verification
│   │   │   ├── notification/    # Notifications (Email, SMS, Push)
│   │   │   ├── admin/           # Admin operations
│   │   │   └── integration/     # External integrations (eSIMGo, Stripe)
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-staging.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/     # Flyway migration scripts
│   └── test/                    # Test classes
├── pom.xml
└── README.md
```

## 🔐 Security

- JWT-based authentication
- OTP login support
- Spring Security configuration
- Role-based access control (ready for implementation)

## 📊 Database Management

### Flyway Migrations

Database schema is managed through Flyway migrations:

- Migration scripts are located in `src/main/resources/db/migration/`
- Naming convention: `V{version}__{description}.sql`
- Example: `V1__init.sql`, `V2__add_user_table.sql`

### JPA/Hibernate

- Entities are defined in `domain/` packages
- JPA repositories in `infrastructure/repository/`
- Use `validate` mode in production (Flyway handles schema)

## 🧪 Testing

Run tests with:
```bash
mvn test
```

## 📝 API Endpoints

### Authentication
- `POST /api/auth/otp/request` - Request OTP
- `POST /api/auth/otp/verify` - Verify OTP and get JWT token

### Health Check
- `GET /api/health/db` - Database connection status
- `GET /actuator/health` - Application health

### User Management
- `GET /api/users/{id}` - Get user details
- `PUT /api/users/{id}` - Update user profile

*More endpoints will be available as modules are implemented*

## 🔧 Development

### Adding a New Module

1. Create package structure: `api/`, `application/`, `domain/`, `infrastructure/`
2. Define domain entities in `domain/`
3. Create repository interfaces in `infrastructure/repository/`
4. Implement service layer in `application/`
5. Create REST controllers in `api/`

### Code Style

- Follow Java naming conventions
- Use Lombok for boilerplate reduction
- Keep controllers thin, business logic in services
- Use DTOs for API communication

## 🌍 Environment Profiles

- **dev** - Development environment (default)
- **staging** - Staging environment
- **prod** - Production environment

Switch profiles:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=staging
```

## 📦 Dependencies

Key dependencies:
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Security
- PostgreSQL Driver
- Flyway Core
- Lombok
- JWT (jjwt)
- Stripe Java SDK
- SpringDoc OpenAPI

## 🚢 Deployment

### AWS Deployment

The application is ready for AWS deployment:
- Configure environment variables in AWS
- Set up RDS PostgreSQL instance
- Deploy using Elastic Beanstalk, ECS, or EC2

### Docker (Coming Soon)

Docker configuration will be added for containerized deployment.

## 🤝 Contributing

1. Create a feature branch
2. Make your changes
3. Write/update tests
4. Submit a pull request

## 📄 License

Copyright © TikTel Ltd. UK. All rights reserved.

## 👥 Team

- **Project:** TTelGo eSIM Platform
- **Company:** TikTel Ltd. UK
- **Version:** 0.0.1-SNAPSHOT

## 📞 Support

For issues and questions, please contact the development team.

---

**Built with ❤️ by TikTel Development Team**

