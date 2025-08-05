#!/bin/bash
#vai sul terminale di intellij e scrivi ./test-persistence.sh per testare la persistenza di dockercompose

echo "🧪 TEST PERSISTENZA DATA DOCKER COMPOSE"
echo "========================================"

# Compila l'app
echo "📦 Compilazione applicazione..."
./mvnw clean package -DskipTests

# Avvia Docker Compose
echo "🐳 Avvio Docker Compose..."
docker-compose up -d

# Aspetta che l'app sia pronta
echo "⏰ Attesa avvio applicazione..."
sleep 30

# Test 1: Verifica che l'app funzioni
echo "✅ Test 1: Health check"
curl -f http://localhost:8090/actuator/health || exit 1

# Test 2: Crea dati di test
echo "✅ Test 2: Creazione categoria di test"
CATEGORY_RESPONSE=$(curl -s -X POST http://localhost:8090/api/categories \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Persistence Test Category",
    "description": "Category created for persistence testing",
    "active": true
  }')

echo "Categoria creata: $CATEGORY_RESPONSE"

# Test 3: Verifica che i dati esistano
echo "✅ Test 3: Verifica esistenza dati"
CATEGORIES_BEFORE=$(curl -s http://localhost:8090/api/categories)
echo "Categorie prima del restart: $CATEGORIES_BEFORE"

# Test 4: Riavvio solo applicazione (database rimane attivo)
echo "✅ Test 4: Restart solo applicazione"
docker-compose restart app
sleep 20

CATEGORIES_AFTER_RESTART=$(curl -s http://localhost:8090/api/categories)
echo "Categorie dopo restart app: $CATEGORIES_AFTER_RESTART"

# Test 5: DOWN completo e UP (test persistenza volumi)
echo "✅ Test 5: DOWN completo e UP (test critico!)"
docker-compose down
docker-compose up -d
sleep 30

CATEGORIES_AFTER_DOWN_UP=$(curl -s http://localhost:8090/api/categories)
echo "Categorie dopo DOWN/UP completo: $CATEGORIES_AFTER_DOWN_UP"

# Verifica risultati
if [[ $CATEGORIES_AFTER_DOWN_UP == *"Persistence Test Category"* ]]; then
    echo "🎉 SUCCESS: I dati sono persistiti attraverso DOWN/UP completo!"
else
    echo "❌ FAILURE: I dati sono stati persi nel DOWN/UP completo!"
    exit 1
fi

# Pulizia
echo "🧹 Pulizia..."
docker-compose down

echo "✨ Test persistenza completato con successo!"