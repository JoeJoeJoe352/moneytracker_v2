import heroImg from './assets/moneytracker_large_logo.png' 
import './App.css'
import 'bootstrap/dist/css/bootstrap.min.css'
import { useState } from 'react'
import { LoginModal } from './features/auth/LoginModal'
import { RegistrationModal } from './features/auth/RegistrationModal'

function App() {
  const [loginOpen, setLoginOpen] = useState(false);
  const [registrationOpen, setRegistrationOpen] = useState(false);
  return (
    <>
      <section id="center">
        <div className="hero">
          <img src={heroImg} className="base" width="170" height="179" alt="" />
        </div>
      </section>

      {/* Login Modal */}
      <button onClick={() => {setLoginOpen(true)}}>Login</button>
      <LoginModal open={loginOpen} onClose={() => setLoginOpen(false)} />

      {/* Registration Modal */}
      <button onClick={() => {setRegistrationOpen(true)}}>Register</button>
      <RegistrationModal open={registrationOpen} onClose={() => setRegistrationOpen(false)} />
      
      <section id="spacer"></section>
    </>
  ) 
}

export default App


