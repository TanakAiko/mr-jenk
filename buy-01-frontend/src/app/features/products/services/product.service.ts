import { Injectable } from "@angular/core";
import { catchError, Observable, of, throwError } from "rxjs";
import { HttpClient } from "@angular/common/http";
import { ProductModels } from "../models/product.models";
import { environment } from "../../../../environments/environment";

@Injectable({
  providedIn: "root",
})
export class ProductService {
  private apiUrl = environment.apiUrl;

  constructor(private httpClient: HttpClient) {}

  getProductList(search?: string): Observable<ProductModels[]> {
    let url = `${this.apiUrl}/api/products`;
    if (search) {
      url += `?search=${encodeURIComponent(search)}`;
    }
    return this.httpClient
      .get<ProductModels[]>(url)
      .pipe(catchError((err) => throwError(() => err)));
  }

  getOneProduct(id: string): Observable<ProductModels> {
    // const xender =
    return this.httpClient
      .get<ProductModels>(`${this.apiUrl}/api/products/${id}`)
      .pipe(catchError((err) => throwError(() => err)));

    // return xender;
  }
}
