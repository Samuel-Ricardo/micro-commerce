import { Environment } from './interface/environment';

export const environment: Environment = {
  production: false,
  AUTH: {
    AUTHORITY:
      'http://localhost:8181/realms/java-shop-microservices-security-realm',
    CLIENT: {
      ID: 'frontend-client',
    },
  },
  SERVICES: {
    PRODUCT: {
      URL: 'http://api-gateway:9000/api/v1/products',
    },
    ORDER: {
      URL: 'http://api-gateway:9000/api/v1/orders',
    },
    INVENTORY: {
      URL: 'http://api-gateway:9000/api/v1/inventory',
    },
  },
};
