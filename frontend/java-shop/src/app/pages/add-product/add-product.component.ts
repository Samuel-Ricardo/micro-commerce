import { NgIf } from '@angular/common';
import { Component, inject } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ProductServiceService } from '../../application/service/product-service.service';
import { Product } from '../../domain/model/product';

@Component({
  selector: 'app-add-product',
  standalone: true,
  imports: [ReactiveFormsModule, NgIf],
  templateUrl: './add-product.component.html',
  styleUrl: './add-product.component.scss',
})
export class AddProductComponent {
  private readonly productService = inject(ProductServiceService);

  addProductForm: FormGroup;
  productCreated: boolean = false;

  constructor(private fb: FormBuilder) {
    this.addProductForm = this.fb.group({
      skuCode: ['', [Validators.required]],
      name: ['', [Validators.required]],
      description: ['', [Validators.required]],
      price: [0, [Validators.required]],
    });
  }

  onSubmit() {
    if (!this.addProductForm.valid) return console.log('Invalid Form');

    const product: Product = {
      skuCode: this.addProductForm.get('skuCode')?.value,
      name: this.addProductForm.get('name')?.value,
      description: this.addProductForm.get('description')?.value,
      price: this.addProductForm.get('price')?.value,
    };

    this.productService.createProduct(product).subscribe((product) => {
      this.productCreated = true;
      this.addProductForm.reset();
    });
  }

  get skuCode() {
    return this.addProductForm.get('skuCode');
  }

  get name() {
    return this.addProductForm.get('name');
  }

  get description() {
    return this.addProductForm.get('description');
  }

  get price() {
    return this.addProductForm.get('price');
  }
}
