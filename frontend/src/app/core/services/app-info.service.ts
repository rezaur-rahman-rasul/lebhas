import { Injectable } from '@angular/core';

import { environment } from '@env/environment';

@Injectable({ providedIn: 'root' })
export class AppInfoService {
  readonly name = environment.appName;
  readonly version = environment.appVersion;
  readonly apiBaseUrl = environment.apiBaseUrl;
  readonly production = environment.production;
}
