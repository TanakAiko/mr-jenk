import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../../../shared/services/toast.service';
import { Order } from '../../models/order.model';
import { Observable, Subject } from 'rxjs';
import { Router } from '@angular/router';
import { LucideAngularModule, Package, Clock, CheckCircle, Truck, XCircle, RotateCcw } from 'lucide-angular';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.css'
})
export class OrderListComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);
  orders$: Observable<Order[]> | undefined;
  searchTerm: string = '';
  private searchSubject = new Subject<string>();

  // Icons
  readonly Package = Package;
  readonly Clock = Clock;
  readonly CheckCircle = CheckCircle;
  readonly Truck = Truck;
  readonly XCircle = XCircle;
  readonly RotateCcw = RotateCcw;

  ngOnInit() {
    this.orders$ = this.orderService.getOrders();
    this.setupSearchSubscription();
  }

  private setupSearchSubscription() {
    this.searchSubject.pipe(
      debounceTime(500),
      distinctUntilChanged()
    ).subscribe(term => {
      if (term.trim()) {
        this.orders$ = this.orderService.searchOrders(term);
      } else {
        this.orders$ = this.orderService.getOrders();
      }
    });
  }

  onSearch(term: string) {
    this.searchTerm = term;
    this.searchSubject.next(term);
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
          this.toastService.success('Order Cancelled', 'Your order has been cancelled successfully.');
        },
        error: (err) => {
          console.error('Error cancelling order', err);
          this.toastService.error('Cancellation Failed', 'Failed to cancel order. Please try again.');
        }
      });
    }
  }

  reorder(orderId: string) {
    if (confirm('Do you want to add these items to your cart?')) {
      this.orderService.reorder(orderId).subscribe({
        next: () => {
          this.router.navigate(['/cart']);
          this.toastService.success('Items Added', 'Items have been added to your cart.');
        },
        error: (err) => {
          console.error('Error reordering', err);
          this.toastService.error('Reorder Failed', 'Failed to reorder items. Please try again.');
        }
      });
    }
  }
}
