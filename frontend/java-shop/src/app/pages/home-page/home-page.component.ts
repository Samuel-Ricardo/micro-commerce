import { Component, OnInit, inject } from '@angular/core';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { ProductServiceService } from '../../application/service/product-service.service';
import { OrderServiceService } from '../../application/service/order-service.service';
import { Router } from '@angular/router';
import { Product } from '../../domain/model/product';
import { Order } from '../../domain/model/order';

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
  orderFailed: boolean = false;

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

  goToCreateProductPage() {
    this.router.navigateByUrl('/add-product');
  }

  orderProduct(product: Product, quantity: string) {
    this.securityService.userData$.subscribe((result) => {
      console.log({ result });

      const userDetails = {
        email: result.userData.email,
        firstName: result.userData.given_name,
        lastName: result.userData.family_name,
      };

      if (!quantity) {
        this.orderFailed = true;
        this.orderSuccess = false;
        this.quantityIsNull = true;
      } else {
        const order: Order = {
          skuCode: product.skuCode,
          price: product.price,
          quantity: Number(quantity),
          userDetails,
        };

        this.ordersService.orderProduct(order).subscribe(
          () => {
            this.orderSuccess = true;
          },
          (error) => (this.orderFailed = true),
        );
      }
    });
  }
}
