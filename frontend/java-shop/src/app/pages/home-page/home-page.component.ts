import { Component, OnInit, inject } from '@angular/core';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { ProductServiceService } from '../../application/service/product-service.service';
import { OrderServiceService } from '../../application/service/order-service.service';
import { Router } from '@angular/router';
import { Product } from '../../domain/model/product';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [],
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.scss',
})
export class HomePageComponent implements OnInit {
  private readonly securityService = inject(OidcSecurityService);
  private readonly productService = inject(ProductServiceService);
  private readonly ordersService = inject(OrderServiceService);
  private readonly router = inject(Router);

  isAuthenticated: boolean = false;
  products: Array<Product> = [];
  quantityIsNull: boolean = false;
  orderSuccess: boolean = false;
  orderFail: boolean = false;

  ngOnInit(): void {
    this.securityService.isAuthenticated$.subscribe(({ isAuthenticated }) => {
      this.isAuthenticated = isAuthenticated;

      this.productService
        .getProducts()
        .pipe()
        .subscribe((products) => {
          this.products = products;
        });
    });
  }
}
