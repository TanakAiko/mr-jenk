import { ApplicationConfig, provideZoneChangeDetection, DEFAULT_CURRENCY_CODE } from "@angular/core";
import { provideRouter } from "@angular/router";
import { provideAnimations } from "@angular/platform-browser/animations";
import { HTTP_INTERCEPTORS } from "@angular/common/http";

import { routes } from "./app.routes";
import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { AuthInterceptor } from "./shared/interceptors/auth.interceptor";

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),
    provideAnimations(),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true,
    },
    { provide: DEFAULT_CURRENCY_CODE, useValue: 'USD' }
  ],
};
