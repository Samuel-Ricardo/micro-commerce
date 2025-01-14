import { Environment } from './interface/environment';

export const environment: Environment = {
  production: true,
  AUTH: {
    AUTHORITY:
      'http://host.docker.internal:8181/realms/java-shop-microservices-security-realm',
    //      'http://localhost:8181/realms/java-shop-microservices-security-realm',
    //      "http://keycloak:8080/realms/java-shop-microservices-security-realm",

    CLIENT: {
      ID: 'frontend-client',
    },
  },
  SERVICES: {
    PRODUCT: {
      URL: 'http://localhost:9000/api/v1/products',
    },
    ORDER: {
      URL: 'http://localhost:9000/api/v1/orders',
    },
    INVENTORY: {
      URL: 'http://localhost:9000/api/v1/inventory',
    },
  },
};
