import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { CartService } from '../../services/cart.service';
import { OrderService } from '../../../orders/services/order.service';
import { ToastService } from '../../../../shared/services/toast.service';
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
  private orderService = inject(OrderService);
  private toastService = inject(ToastService);
  private router = inject(Router);
  
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

  checkout() {
    this.orderService.checkout().subscribe({
      next: (order) => {
        console.log('Checkout successful', order);
        // Clear the cart after successful checkout
        this.cartService.clearCart();
        this.router.navigate(['/orders']);
      },
      error: (err) => {
        console.error('Checkout failed', err);
        this.toastService.error('Checkout Failed', 'Please try again.');
      }
    });
  }
}
