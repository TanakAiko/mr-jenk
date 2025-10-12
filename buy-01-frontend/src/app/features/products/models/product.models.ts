import { MediaModels } from "./media.models";

export interface ProductModels {
  id: string;
  name: string;
  description: string;
  quantity: string;
  price: string;
  userId: string;
  images: MediaModels[];
}
