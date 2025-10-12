import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) => 
      TestBed.runInInjectionContext(() => authGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()]
    });
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
