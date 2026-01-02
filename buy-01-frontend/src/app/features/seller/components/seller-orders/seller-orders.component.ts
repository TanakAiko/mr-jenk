import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../../orders/services/order.service';
import { ToastService } from '../../../../shared/services/toast.service';
import { Order, OrderItem } from '../../../orders/models/order.model';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

@Component({
  selector: 'app-seller-orders',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './seller-orders.component.html',
  styleUrl: './seller-orders.component.css'
})
export class SellerOrdersComponent implements OnInit {
  orders: Order[] = [];
  loading = false;
  error: string | null = null;
  searchTerm: string = '';
  private searchSubject = new Subject<string>();

  readonly orderStatuses = ['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

  constructor(
    private readonly orderService: OrderService,
    private readonly toastService: ToastService
  ) { }

  ngOnInit(): void {
    this.loadOrders();
    this.setupSearchSubscription();
  }

  private setupSearchSubscription() {
    this.searchSubject.pipe(
      debounceTime(500),
      distinctUntilChanged()
    ).subscribe(term => {
      if (term.trim()) {
        this.searchOrders(term);
      } else {
        this.loadOrders();
      }
    });
  }

  onSearch(term: string) {
    this.searchTerm = term;
    this.searchSubject.next(term);
  }

  searchOrders(query: string): void {
    this.loading = true;
    this.orderService.searchSellerOrders(query).subscribe({
      next: (orders) => {
        this.orders = orders;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error searching seller orders', err);
        this.error = 'Failed to search orders. Please try again.';
        this.loading = false;
      }
    });
  }

  loadOrders(): void {
    this.loading = true;
    this.orderService.getSellerOrders().subscribe({
      next: (orders) => {
        this.orders = orders;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading seller orders', err);
        this.error = 'Failed to load orders. Please try again.';
        this.loading = false;
      }
    });
  }

  updateStatus(orderId: string, item: OrderItem, newStatus: string): void {
    if (item.status === newStatus) return;

    this.orderService.updateOrderItemStatus(orderId, item.productId, newStatus).subscribe({
      next: (updatedOrder) => {
        // Update the local order with the new data
        const index = this.orders.findIndex(o => o.id === updatedOrder.id);
        if (index !== -1) {
          this.orders[index] = updatedOrder;
        }
        this.toastService.success('Status Updated', `Order item status updated to ${newStatus}`);
      },
      error: (err) => {
        console.error('Error updating status', err);
        this.toastService.error('Update Failed', 'Failed to update status. Please try again.');
        // Revert the status in UI if needed, but since we bind to the item.status, 
        // and we only update on success or if we used two-way binding we might need to reset.
        // For now, we'll just reload orders to be safe or handle it better.
        this.loadOrders();
      }
    });
  }
}
