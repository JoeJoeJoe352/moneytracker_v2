import { ResourceStatus } from '@angular/core';

/**
 * A resource() státusza az első betöltés alatt (újratöltésnél 'reloading' az érték)
 */
export const RESOURCE_STATUS_LOADING = 'loading' satisfies ResourceStatus;
export const RESOURCE_STATUS_RELOADING = 'reloading' satisfies ResourceStatus;
