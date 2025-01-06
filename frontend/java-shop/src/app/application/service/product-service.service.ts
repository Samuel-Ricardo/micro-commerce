import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Product } from '../../domain/model/product';

@Injectable({
  providedIn: 'root',
})
export class ProductServiceService {
  constructor(private httpClient: HttpClient) {}

  getProducts() {
    return this.httpClient.get<Array<Product>>(
      'http://api-gateway:9000/api/v1/products',
    );
  }

  createProduct(product: Product) {
    return this.httpClient.post<Product>(
      'http://api-gateway:9000/api/v1/products',
      product,
    );
  }
}
