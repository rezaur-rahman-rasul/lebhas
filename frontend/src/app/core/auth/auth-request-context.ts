import { HttpContextToken } from '@angular/common/http';

export const SKIP_AUTH = new HttpContextToken<boolean>(() => false);
export const SKIP_REFRESH = new HttpContextToken<boolean>(() => false);
export const SKIP_ERROR_TOAST = new HttpContextToken<boolean>(() => false);
export const SKIP_GLOBAL_LOADING = new HttpContextToken<boolean>(() => false);
export const RETRY_ONCE = new HttpContextToken<boolean>(() => false);
