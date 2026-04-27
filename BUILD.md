# LabCompare — Build & Deploy Guide

## Prerequisites
- Java 21+
- Maven 3.8+
- Node 18+ & Angular CLI 17+
- MySQL 8+ (for AWS production)

---

## Option A — Quick Start (Local H2 Database)

```bash
cd labcompare
mvn clean package -DskipTests
java -jar target/labcompare.jar
# App running at http://localhost:8080
```

---

## Option B — Full Build (Angular + Spring Boot WAR)

### Step 1 — Build Angular
```bash
cd labcompare-frontend
npm install
ng build --configuration=production
cp -r dist/labcompare-frontend/browser/* ../labcompare/src/main/resources/static/
```

### Step 2 — Build WAR
```bash
cd ../labcompare
mvn clean package -DskipTests
# WAR created at: target/labcompare.war
```

### Step 3 — Test locally with H2
```bash
java -jar target/labcompare.war
# Visit http://localhost:8080
# H2 Console at http://localhost:8080/h2-console
```

---

## Option C — AWS Deployment

### AWS RDS MySQL Setup
1. Create MySQL 8 RDS instance in AWS Console
2. Create database: `CREATE DATABASE labcompare;`
3. Note the endpoint, username, password

### AWS EC2 + Tomcat Setup
```bash
# On EC2 instance:
sudo apt install openjdk-21-jre tomcat10 -y

# Deploy WAR
sudo cp labcompare.war /var/lib/tomcat10/webapps/ROOT.war

# Set environment variables in /etc/environment:
DB_HOST=your-rds-endpoint.rds.amazonaws.com
DB_PORT=3306
DB_NAME=labcompare
DB_USER=admin
DB_PASS=yourpassword

# Activate MySQL profile
# Edit /etc/systemd/system/tomcat10.service, add to [Service]:
Environment="JAVA_OPTS=-Dspring.profiles.active=mysql"

sudo systemctl restart tomcat10
```

### AWS Elastic Beanstalk (Easier)
```bash
# Rename WAR to ROOT.war for root deployment
cp target/labcompare.war labcompare-ROOT.war

# Create .ebextensions/env.config:
option_settings:
  aws:elasticbeanstalk:application:environment:
    SPRING_PROFILES_ACTIVE: mysql
    DB_HOST: your-rds-endpoint
    DB_NAME: labcompare
    DB_USER: admin
    DB_PASS: yourpassword

# Deploy via EB CLI
eb init labcompare --platform java
eb create labcompare-prod
eb deploy
```

---

## API Endpoints Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/labs | List all labs |
| POST | /api/labs | Create lab |
| PUT | /api/labs/{id} | Update lab |
| DELETE | /api/labs/{id} | Delete lab |
| GET | /api/tests?search=cbc | Search tests |
| POST | /api/tests | Create test |
| GET | /api/prices?testId=1&city=Mumbai | Search prices |
| POST | /api/prices | Set price |
| POST | /api/bookings | Create booking |
| GET | /api/bookings/{ref} | Get booking by ref |
| GET | /api/bookings?phone=9999999999 | Get by phone |
| PATCH | /api/bookings/{ref}/cancel | Cancel booking |

---

## Default Seeded Data
- 6 Labs: Dr Lal PathLabs, SRL Diagnostics, Thyrocare, Metropolis, Suburban, Apollo
- 10 Tests: CBC, Thyroid, LFT, Lipid, HbA1c, Vitamin D, KFT, Urine, Dengue, COVID RT-PCR
- 40+ Prices with discounts across labs and cities
