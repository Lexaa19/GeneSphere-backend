# 🧬 GeneSphere Backend

A robust Java-based backend service for genetic data management and analysis.

## 📋 Overview

GeneSphere Backend is a microservices-based application designed to handle genetic information processing, storage, and retrieval. Built with modern Java technologies, it provides a scalable and efficient solution for managing gene-related data.

## 🏗️ Architecture

This project follows a microservices architecture:

- **gene-service**: Core service handling genetic data operations (still work in progress)

- **gene-mutation**: mutation will be another service 

## 🚀 Technologies

- **Language**: Java
- **Build Tool**: Maven
- **Architecture**: Microservices

## 📦 Prerequisites

Before running this application, ensure you have:

- Java JDK 11 or higher
- Maven 3.6+
- Your preferred IDE (IntelliJ IDEA, Eclipse, VS Code)

## 🛠️ Installation
s
1. **Clone the repository**
   ```bash
   git clone https://github.com/Lexaa19/GeneSphere-backend.git
   cd GeneSphere-backend
   ```

2. **Navigate to the service directory**
   ```bash
   cd gene-service
   ```

3. **Build the project**
   ```bash
   mvn clean install
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

## 📁 Project Structure

```
GeneSphere-backend/
├── gene-service/          # Main microservice for gene data management
│   ├── src/              # Source code
│   │   ├── main/
│   │   └── test/
│   └── pom.xml           # Maven configuration
└── .gitignore
```

## 🔧 Configuration

Configuration files can be found in `gene-service/src/main/resources/`. Update the `application.properties` or `application.yml` file with your specific settings:

- Database connection details
- Server port configurations
- API endpoints
- Security settings

## 🧪 Testing

Run the test suite:

```bash
mvn test
```

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 👤 Author

**Lexaa19**

- GitHub: [@Lexaa19](https://github.com/Lexaa19)

---

**Note**: This is a work in progress project. Stay tuned for updates and new features!