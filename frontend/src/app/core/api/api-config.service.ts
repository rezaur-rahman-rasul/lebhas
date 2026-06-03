import { Injectable } from '@angular/core';

import { environment } from '@env/environment';

@Injectable({ providedIn: 'root' })
export class ApiConfigService {
  readonly apiBaseUrl = environment.apiBaseUrl;
}
