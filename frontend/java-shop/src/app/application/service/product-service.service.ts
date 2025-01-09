import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Product } from '../../domain/model/product';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class ProductServiceService {
  constructor(private httpClient: HttpClient) {}

  getProducts() {
    return this.httpClient.get<Array<Product>>(
      environment.SERVICES.PRODUCT.URL || '',
    );
  }

  createProduct(product: Product) {
    return this.httpClient.post<Product>(
      environment.SERVICES.PRODUCT.URL || '',
      product,
    );
  }
}
