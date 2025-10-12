import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MyAccountComponent } from './my-account.component';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

describe('MyAccountComponent', () => {
  let component: MyAccountComponent;
  let fixture: ComponentFixture<MyAccountComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyAccountComponent],
      providers: [
        provideHttpClientTesting(),
        provideHttpClient(),
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of({ get: (_: string) => '1' }),
            snapshot: { paramMap: { get: (_: string) => '1' } }
          }
        }
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(MyAccountComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should create editForm with name and password controls', () => {
    expect(component.editForm.contains('name')).toBeTrue();
    expect(component.editForm.contains('password')).toBeTrue();
  });

  it('should make name required', () => {
    const control = component.editForm.get('name');
    control?.setValue('');
    expect(control?.invalid).toBeTrue();
  });

});
