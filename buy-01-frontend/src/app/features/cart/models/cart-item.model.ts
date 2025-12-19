import { ProductModels } from "../../products/models/product.models";

export interface CartItemModel {
  product: ProductModels;
  quantity: number;
}
