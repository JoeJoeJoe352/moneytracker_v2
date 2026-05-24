import { configureStore } from '@reduxjs/toolkit'
import authReducer from '../features/auth/AuthSlice'

// store -> globális state
// reducer -> a store-ban tárolt state módosításáért felelős függvények gyűjteménye
// slice -> egy reducer és a hozzá tartozó action-ök gyűjteménye, amely egy adott state részért felelős
// actions -> a state módosítását jelző események, amelyek egy adott típusú payload-ot tartalmaznak

// Direkten sose lehet módosítani a store state-et, csak reducer-en keresztül, amely egy új state-et ad vissza a módosítás után
export const store = configureStore({
  reducer: {
    auth: authReducer
  }
})

// A RootState típus a store-ban tárolt state teljes struktúráját reprezentálja, amelyet a reducer-ek határoznak meg
// Így a typescript tudni fogja, hogy milyen típusú adatokat tartalmaz a store
export type RootState = ReturnType<typeof store.getState>

// Az AppDispatch típus a store.dispatch függvény típusát reprezentálja, amelyet a reducer-ek által definiált action-ök dispatch-elésére használunk
export type AppDispatch = typeof store.dispatch