import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { CartItemModel } from '../models/cart-item.model';
import { ProductModels } from '../../products/models/product.models';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private cartItemsSubject = new BehaviorSubject<CartItemModel[]>([]);
  cartItems$ = this.cartItemsSubject.asObservable();
  private apiUrl = `${environment.apiUrl}/api/cart`;

  constructor(private http: HttpClient) {
    this.loadCart();
  }

  private loadCart() {
    this.http.get<any>(this.apiUrl).subscribe({
      next: (response) => {
        if (response && response.items) {
          this.cartItemsSubject.next(response.items);
        }
      },
      error: (err) => {
        console.error('Error loading cart from backend', err);
      }
    });
  }

  private saveCart(items: CartItemModel[]) {
    localStorage.setItem('cart', JSON.stringify(items));
    this.cartItemsSubject.next(items);
  }

  addToCart(product: ProductModels, quantity: number = 1) {
    const currentItems = this.cartItemsSubject.value;
    const existingItem = currentItems.find(item => item.product.id === product.id);

    let updatedItems;
    if (existingItem) {
      updatedItems = currentItems.map(item =>
        item.product.id === product.id
          ? { ...item, quantity: item.quantity + quantity }
          : item
      );
    } else {
      updatedItems = [...currentItems, { product, quantity }];
    }
    this.cartItemsSubject.next(updatedItems);

    this.http.post(`${this.apiUrl}/items/${product.id}`, { quantity }).subscribe({
      next: (response: any) => {
        if (response && response.items) {
          this.cartItemsSubject.next(response.items);
        }
      },
      error: (err) => {
        console.error('Error adding to cart', err);
        this.cartItemsSubject.next(currentItems);
      }
    });
  }

  removeFromCart(productId: string) {
    const currentItems = this.cartItemsSubject.value;
    const updatedItems = currentItems.filter(item => item.product.id !== productId);
    this.cartItemsSubject.next(updatedItems);

    this.http.delete(`${this.apiUrl}/items/${productId}`).subscribe({
      next: (response: any) => {
        if (response && response.items) {
          this.cartItemsSubject.next(response.items);
        }
      },
      error: (err) => {
        console.error('Error removing from cart', err);
        this.cartItemsSubject.next(currentItems);
      }
    });
  }

  updateQuantity(productId: string, quantity: number) {
    const currentItems = this.cartItemsSubject.value;
    const item = currentItems.find(item => item.product.id === productId);
    if (!item) return;

    if (quantity <= 0) {
      this.removeFromCart(productId);
      return;
    }

    const updatedItems = currentItems.map(i =>
      i.product.id === productId ? { ...i, quantity } : i
    );
    this.cartItemsSubject.next(updatedItems);

    this.http.put(`${this.apiUrl}/items/${productId}`, { quantity }).subscribe({
      next: (response: any) => {
        if (response && response.items) {
          this.cartItemsSubject.next(response.items);
        }
      },
      error: (err) => {
        console.error('Error updating quantity', err);
        this.cartItemsSubject.next(currentItems);
      }
    });
  }

  clearCart() {
    const currentItems = this.cartItemsSubject.value;
    this.cartItemsSubject.next([]);

    this.http.delete(this.apiUrl).subscribe({
      next: () => {
      },
      error: (err) => {
        console.error('Error clearing cart', err);
        this.cartItemsSubject.next(currentItems);
      }
    });
  }

  getCartTotal(): number {
    return this.cartItemsSubject.value.reduce((total, item) => {
      return total + (Number(item.product.price) * item.quantity);
    }, 0);
  }
}
