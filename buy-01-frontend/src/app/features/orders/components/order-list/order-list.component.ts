import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';
import { Observable } from 'rxjs';
import { LucideAngularModule, Package, Clock, CheckCircle, Truck, XCircle } from 'lucide-angular';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.css'
})
export class OrderListComponent implements OnInit {
  private orderService = inject(OrderService);
  orders$: Observable<Order[]> | undefined;

  // Icons
  readonly Package = Package;
  readonly Clock = Clock;
  readonly CheckCircle = CheckCircle;
  readonly Truck = Truck;
  readonly XCircle = XCircle;

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
}
