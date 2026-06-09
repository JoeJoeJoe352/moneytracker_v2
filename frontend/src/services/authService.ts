import { apiClient } from '../config/AxiosConfig'

/**
 * Regisztrációs Api hívás
 * 
 * @param username 
 * @param email 
 * @param password 
 * @param passwordAgain 
 * @returns Promise
 */
export const registerUser = (username: string, email: string, password: string, passwordAgain: string) => {
    return apiClient.post('/auth/register', {
        username: username,
        email: email,
        password: password,
        passwordAgain: passwordAgain
    })
}

/**
 * Felhasználónév/email cím foglaltság api hívás
 * 
 * @param username 
 * @param email 
 * @returns Promise
 */
export const isUsernameOrEmailTaken = (username: string, email: string) => {
    return apiClient.post<boolean>('/auth/isUsernameOrEmailExists', {
        username: username,
        email: email
    })
}

/**
 * Felhasználó bejelentkeztetése
 * 
 * @param username 
 * @param password 
 * @returns 
 */
export const loginUser = (username: string, password: string) => {
    return apiClient.post<{token: string}>('/auth/login', {
        username: username,
        password: password
    })
}