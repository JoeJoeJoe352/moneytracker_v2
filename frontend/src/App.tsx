import heroImg from './assets/moneytracker_large_logo.png' 
import RegistrationComponents from './features/auth/RegistrationComponents'
import './App.css'
import 'bootstrap/dist/css/bootstrap.min.css'
import { useState } from 'react'
import { LoginModal } from './features/auth/LoginModal'

function App() {
  const [loginOpen, setLoginOpen] = useState(false);
  return (
    <>
      <section id="center">
        <div className="hero">
          <img src={heroImg} className="base" width="170" height="179" alt="" />
        </div>
      </section>
      <button onClick={() => {setLoginOpen(true)}}>Login</button>
      <LoginModal open={loginOpen} onClose={() => setLoginOpen(false)} />
      <RegistrationComponents />
      <section id="spacer"></section>
    </>
  ) 
}

export default App


