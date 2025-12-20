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
          const mappedItems = this.mapResponseItems(response.items);
          this.cartItemsSubject.next(mappedItems);
        }
      },
      error: (err) => {
        console.error('Error loading cart from backend', err);
      }
    });
  }

  private mapResponseItems(items: any[]): CartItemModel[] {
    return items.map(item => ({
      product: {
        id: item.productId,
        name: item.productName,
        description: '', // Not provided by backend
        quantity: item.availableQuantity ? item.availableQuantity.toString() : '99', // Map availableQuantity from backend
        price: item.priceSnapshot.toString(),
        userId: item.sellerId,
        images: item.imageUrl ? [{ id: '0', imageUrl: item.imageUrl, productId: item.productId }] : []
      },
      quantity: item.quantity
    }));
  }

  private saveCart(items: CartItemModel[]) {
    localStorage.setItem('cart', JSON.stringify(items));
    this.cartItemsSubject.next(items);
  }

  addToCart(product: ProductModels, quantity: number = 1) {
    console.log('Adding to cart:', product, 'Quantity:', quantity);
    const currentItems = this.cartItemsSubject.value;
    console.log('Current cart items:', currentItems);
    
    // Add safety check for item.product
    const existingItem = currentItems.find(item => item.product && item.product.id === product.id);

    let updatedItems;
    if (existingItem) {
      updatedItems = currentItems.map(item =>
        item.product && item.product.id === product.id
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
          const mappedItems = this.mapResponseItems(response.items);
          this.cartItemsSubject.next(mappedItems);
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
    // Add safety check for item.product
    const updatedItems = currentItems.filter(item => item.product && item.product.id !== productId);
    this.cartItemsSubject.next(updatedItems);

    this.http.delete(`${this.apiUrl}/items/${productId}`).subscribe({
      next: (response: any) => {
        if (response && response.items) {
          const mappedItems = this.mapResponseItems(response.items);
          this.cartItemsSubject.next(mappedItems);
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
    // Add safety check for item.product
    const item = currentItems.find(item => item.product && item.product.id === productId);
    if (!item) return;

    if (quantity <= 0) {
      this.removeFromCart(productId);
      return;
    }

    const updatedItems = currentItems.map(i =>
      i.product && i.product.id === productId ? { ...i, quantity } : i
    );
    this.cartItemsSubject.next(updatedItems);

    this.http.put(`${this.apiUrl}/items/${productId}`, { quantity }).subscribe({
      next: (response: any) => {
        if (response && response.items) {
          const mappedItems = this.mapResponseItems(response.items);
          this.cartItemsSubject.next(mappedItems);
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
      // Add safety check for item.product
      if (!item.product) return total;
      return total + (Number(item.product.price) * item.quantity);
    }, 0);
  }
}
