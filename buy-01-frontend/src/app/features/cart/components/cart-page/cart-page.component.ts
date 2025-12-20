import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CartService } from '../../services/cart.service';
import { CartItemModel } from '../../models/cart-item.model';
import { Observable } from 'rxjs';
import { LucideAngularModule, Trash2, Minus, Plus, ShoppingBag, ArrowRight } from 'lucide-angular';

@Component({
  selector: 'app-cart-page',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule],
  templateUrl: './cart-page.component.html',
  styleUrl: './cart-page.component.css'
})
export class CartPageComponent {
  private cartService = inject(CartService);
  
  cartItems$: Observable<CartItemModel[]> = this.cartService.cartItems$;
  
  // Icons
  readonly Trash2 = Trash2;
  readonly Minus = Minus;
  readonly Plus = Plus;
  readonly ShoppingBag = ShoppingBag;
  readonly ArrowRight = ArrowRight;

  updateQuantity(itemId: string, newQuantity: number) {
    if (newQuantity < 1) return;
    this.cartService.updateQuantity(itemId, newQuantity);
  }

  getMaxQuantity(item: CartItemModel): number {
    // Assuming item.product.quantity holds the available stock
    // If it's a string, convert it to a number. Default to a high number if not available.
    return item.product.quantity ? Number(item.product.quantity) : 99;
  }

  removeItem(itemId: string) {
    this.cartService.removeFromCart(itemId);
  }

  getSubtotal(item: CartItemModel): number {
    return Number(item.product.price) * item.quantity;
  }

  get cartTotal(): number {
    return this.cartService.getCartTotal();
  }
}
