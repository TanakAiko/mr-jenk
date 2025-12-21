export interface OrderItem {
  productId: string;
  sellerId: string;
  productName: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
  status: string;
  imageUrl?: string;
}

export interface Order {
  id: string;
  userId: string;
  status: 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
  paymentMode: string;
  totalPrice: number;
  createdAt: string;
  updatedAt: string;
  items: OrderItem[];
}
