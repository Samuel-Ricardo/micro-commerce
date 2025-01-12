import { Component, OnInit, inject } from '@angular/core';
import { OidcSecurityService } from 'angular-auth-oidc-client';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent implements OnInit {
  private readonly securityService = inject(OidcSecurityService);
  isAuthenticated: boolean = false;
  username: string = '';

  ngOnInit(): void {
    this.securityService.isAuthenticated$.subscribe(({ isAuthenticated }) => {
      this.isAuthenticated = isAuthenticated;
    });

    this.securityService.userData$.subscribe(({ userData }) => {
      if (userData) this.username = userData['preferred_username']; // userData.preferred_username;
    });
  }

  login() {
    this.securityService.authorize();
  }

  logout() {
    this.securityService
      .logoff()
      .subscribe((result) => console.log({ result }));
  }
}
