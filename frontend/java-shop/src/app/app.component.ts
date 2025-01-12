import { Component, OnInit, inject } from '@angular/core';
import { RouterModule, RouterOutlet } from '@angular/router';
import { HeaderComponent } from './shared/header/header.component';
import { OidcSecurityService } from 'angular-auth-oidc-client';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterModule, HeaderComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  title = 'java-shop';

  private readonly securityService = inject(OidcSecurityService);

  ngOnInit(): void {
    this.securityService
      .checkAuth()
      .subscribe(({ isAuthenticated }: any) =>
        console.log('App Authenticated: ', isAuthenticated),
      );
  }
}
