import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CategoryResponseInterface, CategorySaveRequestInterface } from './interfaces';

@Injectable({
    providedIn: 'root',
})
export class CategoryService {
    private readonly http = inject(HttpClient);

    /**
     * Kategória létrehozása
     */
    saveCategory(
        categoryData: CategorySaveRequestInterface,
    ): Observable<CategoryResponseInterface> {
        return this.http.post<CategoryResponseInterface>('/api/category', categoryData);
    }

    /**
     * Kategóriák listájának lekérdezése
     */
    listCategories(): Observable<CategoryResponseInterface[]> {
        return this.http.get<CategoryResponseInterface[]>('/api/category');
    }
}
