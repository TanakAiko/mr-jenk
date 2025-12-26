import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../../orders/services/order.service';
import { Order, OrderItem } from '../../../orders/models/order.model';

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
  
  readonly orderStatuses = ['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

  constructor(private orderService: OrderService) {}

  ngOnInit(): void {
    this.loadOrders();
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
      },
      error: (err) => {
        console.error('Error updating status', err);
        alert('Failed to update status');
        // Revert the status in UI if needed, but since we bind to the item.status, 
        // and we only update on success or if we used two-way binding we might need to reset.
        // For now, we'll just reload orders to be safe or handle it better.
        this.loadOrders(); 
      }
    });
  }
}
