import { Environment } from './interface/environment';

export const environment: Environment = {
  production: false,
  AUTH: {
    AUTHORITY: '',
    CLIENT: {
      ID: '',
    },
  },
  SERVICES: {
    PRODUCT: {
      URL: '',
    },
    ORDER: {
      URL: '',
    },
    INVENTORY: {
      URL: '',
    },
  },
};
