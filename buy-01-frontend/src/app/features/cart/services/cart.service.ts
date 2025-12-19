import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { CartItemModel } from '../models/cart-item.model';
import { ProductModels } from '../../products/models/product.models';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private cartItemsSubject = new BehaviorSubject<CartItemModel[]>([]);
  cartItems$ = this.cartItemsSubject.asObservable();

  constructor() {
    this.loadCart();
  }

  private loadCart() {
    const savedCart = localStorage.getItem('cart');
    if (savedCart) {
      this.cartItemsSubject.next(JSON.parse(savedCart));
    }
  }

  private saveCart(items: CartItemModel[]) {
    localStorage.setItem('cart', JSON.stringify(items));
    this.cartItemsSubject.next(items);
  }

  addToCart(product: ProductModels, quantity: number = 1) {
    const currentItems = this.cartItemsSubject.value;
    const existingItem = currentItems.find(item => item.product.id === product.id);

    if (existingItem) {
      existingItem.quantity += quantity;
      this.saveCart([...currentItems]);
    } else {
      this.saveCart([...currentItems, { product, quantity }]);
    }
  }

  removeFromCart(productId: string) {
    const currentItems = this.cartItemsSubject.value;
    const updatedItems = currentItems.filter(item => item.product.id !== productId);
    this.saveCart(updatedItems);
  }

  updateQuantity(productId: string, quantity: number) {
    const currentItems = this.cartItemsSubject.value;
    const item = currentItems.find(item => item.product.id === productId);
    if (item) {
      item.quantity = quantity;
      if (item.quantity <= 0) {
        this.removeFromCart(productId);
      } else {
        this.saveCart([...currentItems]);
      }
    }
  }

  clearCart() {
    this.saveCart([]);
  }

  getCartTotal(): number {
    return this.cartItemsSubject.value.reduce((total, item) => {
      return total + (Number(item.product.price) * item.quantity);
    }, 0);
  }
}
