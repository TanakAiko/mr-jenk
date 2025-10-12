import { Component, inject, OnInit } from "@angular/core";
import { ProductService } from "../../services/product.service";
import { ProductModels } from "../../models/product.models";
import { RouterLink } from "@angular/router";
import { AuthService } from "../../../../auth/services/auth.service";
import { CommonModule } from "@angular/common";
import { JwtService } from "../../../../shared/services/jwt.service";

@Component({
  selector: "app-product-listing",
  imports: [RouterLink, CommonModule],
  templateUrl: "./product-listing.component.html",
  styleUrl: "./product-listing.component.css",
})
export class ProductListingComponent implements OnInit {
  allProducts: ProductModels[] | null = null;
  filteredProducts: ProductModels[] | null = null;
  currentUser: any = null;
  isLoading = false;
  selectedSort: string = "";
  searchTerm: string = "";

  private productService = inject(ProductService);
  private authService = inject(AuthService);
  private jwtService = inject(JwtService);

  // Helper methods for template
  getQuantityAsNumber(quantity: string): number {
    return Number(quantity) || 0;
  }

  getPriceAsNumber(price: string): number {
    return Number(price) || 0;
  }

  ngOnInit() {
    this.checkUserStatus();
    this.loadProducts();
  }

  private checkUserStatus() {
    if (this.authService.isLoggedIn()) {
      const token = this.authService.getToken();
      if (token) {
        this.currentUser = this.jwtService.decodeToken(token);
        if (!this.currentUser) {
          console.error("Error parsing user data");
        }
      }
    }
  }

  private loadProducts() {
    this.isLoading = true;
    this.productService.getProductList().subscribe({
      next: (value) => {
        console.log("Products loaded successfully:");
        this.allProducts = value;
        this.filteredProducts = value;
        this.isLoading = false;
        console.log(value);
      },
      error: (err) => {
        console.error("Error loading products:", err);
        console.log("Test the webhook");
        
        this.isLoading = false;
        this.allProducts = [];
        this.filteredProducts = [];
      },
    });
  }

  get isGuest(): boolean {
    return !this.authService.isLoggedIn();
  }

  get isClient(): boolean {
    return this.currentUser?.role === "CLIENT";
  }

  get isSeller(): boolean {
    return this.currentUser?.role === "SELLER";
  }

  addToCart(product: ProductModels) {
    if (this.isGuest) {
      // Enhanced guest experience
      this.showLoginPrompt();
      return;
    }

    // Enhanced cart logic
    console.log("Adding to cart:", product);
    this.showSuccessMessage(`${product.name} added to cart!`);

    // Here you would typically call a cart service
    // this.cartService.addToCart(product);
  }

  formatPrice(price: number | string): string {
    const numericPrice = typeof price === "string" ? parseFloat(price) : price;
    return new Intl.NumberFormat("fr-SN", {
      style: "currency",
      currency: "XOF",
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(numericPrice);
  }

  // Enhanced user interaction methods
  private showLoginPrompt() {
    // You can replace this with a more sophisticated modal or toast
    const shouldRedirect = confirm(
      "Please login to add items to your cart. Would you like to go to the login page now?",
    );
    if (shouldRedirect) {
      // Navigate to auth page
      window.location.href = "/auth";
    }
  }

  private showSuccessMessage(message: string) {
    // You can replace this with a toast notification service
    console.log(message);
    // Example: this.toastr.success(message);
  }

  // Enhanced product filtering methods (you can add these features)
  filterByCategory(category: string) {
    // Implementation for category filtering
    console.log(`Filtering by category: ${category}`);
  }

  sortProducts(sortBy: "name" | "price") {
    if (!this.filteredProducts) return;

    this.selectedSort = sortBy;
    this.filteredProducts.sort((a, b) => {
      switch (sortBy) {
        case "name":
          return a.name.localeCompare(b.name);
        case "price":
          return Number(a.price) - Number(b.price);
        default:
          return 0;
      }
    });
  }

  // Method to refresh products
  refreshProducts() {
    this.allProducts = null;
    this.filteredProducts = null;
    this.selectedSort = "";
    this.searchTerm = "";
    this.loadProducts();
  }

  // Search functionality
  onSearch(term: string) {
    this.searchTerm = term.toLowerCase();
    this.filterProducts();
  }

  private filterProducts() {
    if (!this.allProducts) return;

    if (!this.searchTerm) {
      this.filteredProducts = [...this.allProducts];
    } else {
      this.filteredProducts = this.allProducts.filter(
        (product) =>
          product.name.toLowerCase().includes(this.searchTerm) ||
          (product.description &&
            product.description.toLowerCase().includes(this.searchTerm)),
      );
    }

    // Reapply sort if one was selected
    if (this.selectedSort) {
      this.sortProducts(this.selectedSort as "name" | "price");
    }
  }

  // Track by function for better performance
  trackByProductId(index: number, product: ProductModels): string {
    return product.id;
  }

  // Check if product is new (created within last 30 days)
  isNewProduct(product: ProductModels): boolean {
    // This would typically check a createdAt date
    // For now, return false as we don't have that field
    return false;
  }
}
