import heroImg from './assets/moneytracker_large_logo.png' 
import RegistrationComponents from './features/auth/RegistrationComponents'
import './App.css'
import 'bootstrap/dist/css/bootstrap.min.css'

function App() {
  return (
    <>
      <section id="center">
        <div className="hero">
          <img src={heroImg} className="base" width="170" height="179" alt="" />
        </div>
      </section>

      <RegistrationComponents />
      <section id="spacer"></section>
    </>
  )
}

export default App
