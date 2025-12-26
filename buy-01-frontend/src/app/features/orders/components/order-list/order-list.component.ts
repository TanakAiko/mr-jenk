import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';
import { Observable } from 'rxjs';
import { Router } from '@angular/router';
import { LucideAngularModule, Package, Clock, CheckCircle, Truck, XCircle, RotateCcw } from 'lucide-angular';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.css'
})
export class OrderListComponent implements OnInit {
  private orderService = inject(OrderService);
  private router = inject(Router);
  orders$: Observable<Order[]> | undefined;

  // Icons
  readonly Package = Package;
  readonly Clock = Clock;
  readonly CheckCircle = CheckCircle;
  readonly Truck = Truck;
  readonly XCircle = XCircle;
  readonly RotateCcw = RotateCcw;

  ngOnInit() {
    this.orders$ = this.orderService.getOrders();
  }

  getStatusIcon(status: string) {
    switch (status) {
      case 'PENDING': return this.Clock;
      case 'CONFIRMED': return this.CheckCircle;
      case 'SHIPPED': return this.Truck;
      case 'DELIVERED': return this.Package;
      case 'CANCELLED': return this.XCircle;
      default: return this.Clock;
    }
  }

  getStatusClass(status: string): string {
    return `status-${status.toLowerCase()}`;
  }

  cancelOrder(orderId: string) {
    if (confirm('Are you sure you want to cancel this order?')) {
      this.orderService.cancelOrder(orderId).subscribe({
        next: () => {
          // Refresh orders
          this.orders$ = this.orderService.getOrders();
        },
        error: (err) => {
          console.error('Error cancelling order', err);
          alert('Failed to cancel order');
        }
      });
    }
  }

  reorder(orderId: string) {
    if (confirm('Do you want to add these items to your cart?')) {
      this.orderService.reorder(orderId).subscribe({
        next: () => {
          this.router.navigate(['/cart']);
        },
        error: (err) => {
          console.error('Error reordering', err);
          alert('Failed to reorder items');
        }
      });
    }
  }
}
