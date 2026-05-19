import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../services/auth';

@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dashboard-home.html',
  styleUrls: ['./dashboard-home.css']
})
export class DashboardHomeComponent {
  username = '';
  isAdmin = false;

  constructor(private authService: AuthService) {
    this.username = this.authService.getUsername();
    this.isAdmin = this.authService.isAdmin();
  }
}
