import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Order } from '../../domain/model/order';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class OrderServiceService {
  constructor(private httpClient: HttpClient) {}

  orderProduct(order: Order): Observable<string> {
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
      }),
      responseType: 'text' as 'json',
    };

    return this.httpClient.post<string>(
      'http://api-gateway:9000/api/v1/orders',
      order,
      httpOptions,
    );
  }
}
