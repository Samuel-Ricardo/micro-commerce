# Pull Docker images
docker pull samuelrcabral/javashop_frontend:latest
docker pull quay.io/keycloak/keycloak:24.0.1
docker pull mysql:8
docker pull samuelrcabral/api-gateway:latest
docker pull samuelrcabral/notification-service:latest
docker pull samuelrcabral/product-service:latest
docker pull samuelrcabral/order-service:latest
docker pull samuelrcabral/inventory-service:latest
docker pull confluentinc/cp-zookeeper:latest
docker pull confluentinc/cp-kafka:latest
docker pull confluentinc/cp-schema-registry:latest
docker pull provectuslabs/kafka-ui:latest
docker pull grafana/loki:main
docker pull prom/prometheus:v2.46.0
docker pull grafana/tempo:2.2.2
docker pull grafana/grafana:10.1.0
docker pull mongo:latest
docker pull mongo-express:latest
docker pull postgres:latest
docker pull dockerframework/pgadmin:latest
docker pull phpmyadmin:latest

# Load images into kind cluster
kind load docker-image -n java-shop samuelrcabral/javashop_frontend:latest
kind load docker-image -n java-shop quay.io/keycloak/keycloak:24.0.1
kind load docker-image -n java-shop mysql:8
kind load docker-image -n java-shop samuelrcabral/api-gateway:latest
kind load docker-image -n java-shop samuelrcabral/notification-service:latest
kind load docker-image -n java-shop samuelrcabral/product-service:latest
kind load docker-image -n java-shop samuelrcabral/order-service:latest
kind load docker-image -n java-shop samuelrcabral/inventory-service:latest
kind load docker-image -n java-shop confluentinc/cp-zookeeper:latest
kind load docker-image -n java-shop confluentinc/cp-kafka:latest
kind load docker-image -n java-shop confluentinc/cp-schema-registry:latest
kind load docker-image -n java-shop provectuslabs/kafka-ui:latest
kind load docker-image -n java-shop grafana/loki:main
kind load docker-image -n java-shop prom/prometheus:v2.46.0
kind load docker-image -n java-shop grafana/tempo:2.2.2
kind load docker-image -n java-shop grafana/grafana:10.1.0
kind load docker-image -n java-shop mongo:latest
kind load docker-image -n java-shop mongo-express:latest
kind load docker-image -n java-shop postgres:latest
kind load docker-image -n java-shop dockerframework/pgadmin:latest
kind load docker-image -n java-shop phpmyadmin:latest
