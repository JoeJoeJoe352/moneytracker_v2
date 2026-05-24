import { createSlice } from "@reduxjs/toolkit"

interface AuthState {
    token: string | null
    username: string
}

const initialState: AuthState = {
    token: null,
    username: '',
}

const authSlice = createSlice({
    name: 'auth',
    initialState,
    reducers: {
        setCredentials: (state, action) => {
            state.token = action.payload.token
            state.username = action.payload.username
        },
        logout: (state) => {
            state.token = null
            state.username = ''
        }
    }
})
export const { setCredentials, logout } = authSlice.actions

export default authSlice.reducer