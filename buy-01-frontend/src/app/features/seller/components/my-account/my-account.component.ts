import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { LucideAngularModule, Edit3, User, Mail, Camera, Save, X, Key } from 'lucide-angular';
import { AuthService } from '../../../../auth/services/auth.service';
import { ToastService } from '../../../../shared/services/toast.service';
import { UserService, UserProfile } from '../../../../shared/services/user.service';

@Component({
  selector: 'app-my-account',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './my-account.component.html',
  styleUrl: './my-account.component.css'
})
export class MyAccountComponent implements OnInit, OnDestroy {
  // Icons
  readonly Edit3 = Edit3;
  readonly User = User;
  readonly Mail = Mail;
  readonly Camera = Camera;
  readonly Save = Save;
  readonly X = X;
  readonly Key = Key;

  userProfile: UserProfile | null = null;
  editForm: FormGroup;
  isEditMode = false;
  isLoading = false;
  selectedFile: File | null = null;
  imagePreview: string | null = null;
  private destroy$ = new Subject<void>();

  constructor(
    private authService: AuthService,
    private userService: UserService,
    private formBuilder: FormBuilder,
    private router: Router,
    private toastService: ToastService
  ) {
    this.editForm = this.createEditForm();
  }

  ngOnInit(): void {
    this.loadUserProfile();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private createEditForm(): FormGroup {
    return this.formBuilder.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      password: ['', [Validators.minLength(3)]] // Optional password field
    });
  }

  private loadUserProfile(): void {
    this.isLoading = true;
    this.userService.getCurrentUserProfile()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (userProfile) => {
          console.log('User profile loaded:', userProfile);
          this.userProfile = userProfile;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading user profile:', error);
          this.toastService.showError(error.message || 'Failed to load user profile');
          this.isLoading = false;
          
          // Fallback to AuthService if UserService fails
          this.loadUserProfileFromAuth();
        }
      });
  }

  private loadUserProfileFromAuth(): void {
    this.authService.getCurrentUser()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (user) => {
          this.userProfile = {
            id: user.id,
            name: user.name,
            email: user.email,
            role: user.role,
            avatar: user.avatar
          };
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading user profile from auth:', error);
          this.toastService.showError('Failed to load user profile');
          this.isLoading = false;
        }
      });
  }

  toggleEditMode(): void {
    if (this.isEditMode) {
      this.cancelEdit();
    } else {
      this.startEdit();
    }
  }

  private startEdit(): void {
    if (this.userProfile) {
      this.editForm.patchValue({
        name: this.userProfile.name,
        password: '' // Always start with empty password
      });
      this.isEditMode = true;
    }
  }

  cancelEdit(): void {
    this.isEditMode = false;
    this.editForm.reset();
    this.selectedFile = null;
    this.imagePreview = null;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      
      // Check permission first
      if (!this.canEditAvatar()) {
        this.toastService.showError('You do not have permission to change your avatar');
        // Reset the input
        input.value = '';
        return;
      }
      
      // Validate file type
      if (!file.type.startsWith('image/')) {
        this.toastService.showError('Please select a valid image file');
        return;
      }

      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.toastService.showError('File size must be less than 5MB');
        return;
      }

      this.selectedFile = file;

      // Create preview
      const reader = new FileReader();
      reader.onload = (e) => {
        this.imagePreview = e.target?.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  saveProfile(): void {
    if (this.editForm.valid) {
      this.isLoading = true;
      const formData = this.editForm.value;

      // Determine if we need to upload a file or just update text fields
      if (this.selectedFile) {
        // Update with file upload
        const updateData = {
          name: formData.name,
          password: formData.password || undefined, // Only include if not empty
          avatarFile: this.selectedFile
        };

        this.userService.updateUserProfileWithFile(updateData)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (updatedProfile) => {
              this.userProfile = updatedProfile;
              this.isLoading = false;
              this.isEditMode = false;
              this.selectedFile = null;
              this.imagePreview = null;
              this.toastService.showSuccess('Profile updated successfully!');
            },
            error: (error) => {
              console.error('Error updating profile with file:', error);
              this.toastService.showError(error.message || 'Failed to update profile');
              this.isLoading = false;
            }
          });
      } else {
        // Update text fields only
        const updateData = {
          name: formData.name,
          password: formData.password || undefined // Only include if not empty
        };

        this.userService.updateUserProfile(updateData)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (updatedProfile) => {
              this.userProfile = updatedProfile;
              this.isLoading = false;
              this.isEditMode = false;
              this.toastService.showSuccess('Profile updated successfully!');
            },
            error: (error) => {
              console.error('Error updating profile:', error);
              this.toastService.showError(error.message || 'Failed to update profile');
              this.isLoading = false;
            }
          });
      }
    } else {
      this.markFormGroupTouched();
    }
  }

  private markFormGroupTouched(): void {
    Object.keys(this.editForm.controls).forEach(key => {
      this.editForm.get(key)?.markAsTouched();
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.editForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  getFieldError(fieldName: string): string {
    const field = this.editForm.get(fieldName);
    if (field?.errors) {
      if (field.errors['required']) {
        return `${fieldName.charAt(0).toUpperCase() + fieldName.slice(1)} is required`;
      }
      if (field.errors['email']) {
        return 'Please enter a valid email address';
      }
      if (field.errors['minlength']) {
        const requiredLength = field.errors['minlength'].requiredLength;
        if (fieldName === 'password') {
          return `Password must be at least ${requiredLength} characters`;
        }
        return `${fieldName.charAt(0).toUpperCase() + fieldName.slice(1)} must be at least ${requiredLength} characters`;
      }
    }
    return '';
  }

  getAvatarUrl(): string {
    if (this.imagePreview) {
      return this.imagePreview;
    }
    if (this.userProfile?.avatar && this.userProfile.avatar !== 'path/to/image') {
      return this.userProfile.avatar;
    }
    return '';
  }

  getRoleDisplayName(): string {
    if (!this.userProfile?.role) return '';
    return this.userProfile.role.charAt(0).toUpperCase() + this.userProfile.role.slice(1).toLowerCase();
  }

  canEditAvatar(): boolean {
    // Only sellers can edit their avatar
    return this.userProfile?.role === 'SELLER';
  }

  goBack(): void {
    // Navigate back based on user role
    if (this.userProfile?.role === 'SELLER') {
      this.router.navigate(['/seller/my-products']);
    } else {
      this.router.navigate(['/products']);
    }
  }

  removeAvatar(): void {
    if (!this.userProfile?.avatar) {
      this.toastService.showWarning('No avatar to remove');
      return;
    }

    if (!this.canEditAvatar()) {
      this.toastService.showError('You do not have permission to change your avatar');
      return;
    }

    this.isLoading = true;
    this.userService.deleteAvatar()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updatedProfile) => {
          this.userProfile = updatedProfile;
          this.isLoading = false;
          this.selectedFile = null;
          this.imagePreview = null;
          this.toastService.showSuccess('Avatar removed successfully!');
        },
        error: (error) => {
          console.error('Error removing avatar:', error);
          this.toastService.showError(error.message || 'Failed to remove avatar');
          this.isLoading = false;
        }
      });
  }
}
