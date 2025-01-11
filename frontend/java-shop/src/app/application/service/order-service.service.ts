import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Order } from '../../domain/model/order';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

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
      environment.SERVICES.ORDER.URL || '/',
      order,
      httpOptions,
    );
  }
}
