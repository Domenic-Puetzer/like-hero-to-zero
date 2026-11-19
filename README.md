
# Like Hero To Zero 🌍

**Case Study Project:**  
Web application to visualize global CO₂ emissions. Includes a public frontend and a backend for researchers to contribute or correct data.

---

## 🚀 Getting Started


### Prerequisites

- Java 21+
- Maven

### Deployment to Microsoft Azure

If you want to set up the application in Microsoft Azure using Terraform,
please follow the instructions in the
[Terraform README](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/terraform/README.md).

#### 1. Create a new MySQL database
    Before starting the application, you need to create a MySQL database (e.g. using TablePlus: https://tableplus.com/ or any other MySQL client): You can choose any name (e.g. `likeherotozero`).

#### 2. Clone the repository
    git clone https://github.com/Domenic-Puetzer/like-hero-to-zero.git
    cd like-hero-to-zero

#### 3. Copy and rename the environment file
    Rename `env-example` to `.env` in the project root.

#### 4. Configure your database credentials
    Open the `.env` file and enter your MySQL database name, username, and password.


#### 5. Start the backend (Spring Boot)
    ./mvnw spring-boot:run
#### or:
    .\mvnw.cmd spring-boot:run

- The app will be available at [http://localhost:8080](http://localhost:8080)

---

## 🔧 Technologies

- Spring Boot (Java)
- Spring Data JPA (Hibernate)
- Thymeleaf
- MySQL
- Spring Security
- Tailwind CSS
- Vanilla JavaScript (ES6 Modules)
- Leaflet.js (for map visualization)

---


## 📦 Features

- Public display of a country's CO₂ emissions on an interactive world map
- User registration and login system (User, Scientist roles)
- Data upload, modification, and peer review for scientists
- Export and statistics functions
- Responsive design

---

## 🛠️ To Be Done

- Admin role and permissions
- Admin approval workflow for new or updated entries

---

## 📁 Project Structure

```
src/
 ├─ main/
 │   ├─ java/
 │   │   └─ de/likeherotozero/        # Java backend (Spring Boot)
 │   ├─ resources/
 │   │   ├─ static/                   # Static assets (JS, CSS, images)
 │   │   ├─ templates/                # Thymeleaf HTML templates
 │   │   └─ application.properties    # Configuration
 ├─ test/                             # Tests
docs/                                 # Additional documentation
```

---

## 🧑‍💻 Development

- Backend: Spring Boot, JPA, Security
- Frontend: Thymeleaf, Tailwind CSS, Vanilla JS, Leaflet.js
- See [docs/architecture.md](docs/architecture.md) for an overview.

---

## 📝 Documentation

- [docs/architecture.md](docs/architecture.md) – Project architecture and technical overview
- [docs/user-guide.md](docs/user-guide.md) – User guide for general users
- [docs/scientist-guide.md](docs/scientist-guide.md) – Scientist dashboard and workflow
- [docs/api.md](docs/api.md) – API documentation (REST endpoints)
---

## 🌍 Emission Data Sources

- Hannah Ritchie, Pablo Rosado, and Max Roser (2023) – “CO₂ and Greenhouse Gas Emissions”  
Published online at OurWorldinData.org. https://ourworldindata.org/co2-and-greenhouse-gas-emissions
- **GitHub:** [https://github.com/owid/co2-data](https://github.com/owid/co2-data)

---

## 📄 License

This project is provided for educational purposes as part of a university case study.
**All rights reserved.**
Use, distribution, or modification of the code is only permitted for university staff and examiners for review purposes.
Any other use is prohibited without explicit permission.