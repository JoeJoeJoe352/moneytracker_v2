import heroImg from './assets/moneytracker_large_logo.png' 
import RegistrationComponents from './features/auth/RegistrationComponents'
import './App.css'
import 'bootstrap/dist/css/bootstrap.min.css'
import LoginComponent from './features/auth/LoginComponent'
import type { RootState } from './store/store'
import { useSelector } from 'react-redux'

function App() {
  const token = useSelector((state: RootState) => state.auth.token)
  return (
    <>
      <section id="center">
        <div className="hero">
          <img src={heroImg} className="base" width="170" height="179" alt="" />
        </div>
      </section>
      token: {token}
      <LoginComponent/>
      <RegistrationComponents />
      <section id="spacer"></section>
    </>
  )
}

export default App
