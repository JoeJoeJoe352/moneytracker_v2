import { ResourceStatus } from '@angular/core';

/**
 * A resource() státusza az első betöltés alatt (újratöltésnél 'reloading' az érték)
 */
export const RESOURCE_STATUS_LOADING = 'loading' satisfies ResourceStatus;
export const RESOURCE_STATUS_RELOADING = 'reloading' satisfies ResourceStatus;

/**
 * Alkalmazásban használt regex az email validáláshoz
 */
export const STRICT_EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;