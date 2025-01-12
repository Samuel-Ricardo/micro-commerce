export interface Environment {
  production: boolean;
  AUTH: {
    AUTHORITY: string;
    CLIENT: {
      ID: string;
    };
  };
  SERVICES: {
    PRODUCT: {
      URL: string;
    };
    ORDER: {
      URL: string;
    };
    INVENTORY: {
      URL: string;
    };
  };
}
