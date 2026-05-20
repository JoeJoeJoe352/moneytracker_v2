import { apiClient } from '../config/AxiosConfig'

export const registerUser = (username: string, email: string, password: string, passwordAgain: string) => {
    return apiClient.post('/auth/register', {
        username: username,
        email: email,
        password: password,
        passwordAgain: passwordAgain
    })
}