import { Formik } from "formik"
import { loginUser } from "../../services/authService"
import { useDispatch } from "react-redux"
import { setCredentials } from "./AuthSlice"

export const LoginComponent = () => {
    const dispatch = useDispatch()

    function formikOnSubmit(values: { username: string, password: string }) {
        loginUser(values.username, values.password).then(response => {
            dispatch(setCredentials({ token: response.data.token }))
        }).catch(error => {
            console.error('Login error:', error)
        })
    }

    /**
     * Felhasználónév jelszó páros ellenőrzése
     * 
     * @param values 
     * @returns 
     */
    function formikValidate(values: { username: string, password: string }): { [key: string]: string } {
        const errors: { [key: string]: string } = {}
        if (values.username.trim() === '') {
            errors.username = 'Username is required'
        }
        if (values.password.trim() === '') {
            errors.password = 'Password is required'
        }
        return errors
    }

    return (
        <>
            <h1>Login</h1>
            <Formik
                initialValues={{  username: "", password: ""}}
                onSubmit={formikOnSubmit}
                validate={formikValidate}
                validateOnChange={false}
            >
                {({values, handleChange, handleSubmit, errors }) => (
                    <form className="form" onSubmit={ handleSubmit }>
                        <div className="form-group">
                            <label htmlFor="username">Username:</label>
                            <input
                                type="text"
                                id="username"
                                name="username"
                                value={values.username}
                                className="form-control"
                                onChange={handleChange}
                            />
                            {errors.username ? <div>{errors.username}</div> : null}
                        </div>
                        <div className="form-group">
                            <label htmlFor="password">Password:</label>
                            <input
                                type="password"
                                id="password"
                                name="password"
                                value={values.password}
                                className="form-control"
                                onChange={handleChange}
                            />
                            {errors.password ? <div>{errors.password}</div> : null}
                        </div>
                        <div>
                            <input type="submit" value="Login" className="btn btn-primary" />
                        </div>
                    </form>
                )}
            </Formik>
        </>
    )
}

export default LoginComponent