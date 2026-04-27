#!/bin/bash
set -e
echo "==============================="
echo "  LabCompare Deploy Script"
echo "==============================="

# Step 1: Build Angular
echo ""
echo ">> Step 1: Building Angular frontend..."
cd ../labcompare-frontend
npm install --silent
ng build --configuration=production
cp -r dist/labcompare-frontend/browser/* ../labcompare/src/main/resources/static/
echo "   Angular build complete."

# Step 2: Build Spring Boot WAR
echo ""
echo ">> Step 2: Building Spring Boot WAR..."
cd ../labcompare
mvn clean package -DskipTests -q
echo "   WAR built at: target/labcompare.war"

# Step 3: Summary
echo ""
echo "==============================="
echo "  BUILD SUCCESSFUL"
echo "==============================="
echo ""
echo "To run locally (H2 DB):"
echo "  java -jar target/labcompare.war"
echo ""
echo "To run with MySQL (AWS):"
echo "  export DB_HOST=your-rds-host"
echo "  export DB_USER=admin"
echo "  export DB_PASS=password"
echo "  export DB_NAME=labcompare"
echo "  java -jar target/labcompare.war --spring.profiles.active=mysql"
echo ""
echo "App URL: http://localhost:8080"
echo ""
