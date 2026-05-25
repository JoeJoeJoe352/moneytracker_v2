import { Formik } from "formik";
import { registerUser, isUsernameOrEmailTaken } from "../../services/authService";

interface RegistrationValues {
    username: string
    email: string
    password: string
    passwordAgain: string
}

function RegistrationComponents() {
    /**
     * Formik számára a validációs függvény
     */
    async function formikValidate(values: RegistrationValues): Promise<{ [key: string]: string }> {
        const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
        const errors: { [key: string]: string } = {}
        if (!values.username || values.username.length < 3) {
            errors.username = 'Username must be at least 3 characters long'
        }
        if (!values.email || values.email.length < 5 || values.email.length > 100) {
            errors.email = 'Email must be between 5 and 100 characters long'
        } else if(!emailRegex.test(values.email)) {
            errors.email = 'Email is not valid'
        }
        if (!values.password || values.password.length < 6 || values.password.length > 100) {
            errors.password = 'Password must be between 6 and 100 characters long'
        }
        if (values.password !== values.passwordAgain) {
            errors.passwordAgain = 'Passwords do not match'
        }

        // Ha nincs még hiba, akkor megnézzük foglalt-e az email cím/username
        if (Object.keys(errors).length === 0) {
            const isUsernameOrEmailTaken = await checkUsernameOrEmailAvailability(values.username, values.email)
            if (isUsernameOrEmailTaken) {
                errors.username = 'Username or email is already taken'
            }
        }
        console.log(errors)
        return errors
    }

    async function checkUsernameOrEmailAvailability(username: string, email: string): Promise<boolean> {
        try {
            const response = await isUsernameOrEmailTaken(username, email);
            return response.data === true
        } catch (error) {
            console.error('Error checking username/email', error)
            return true
        }
    }

    /**
     * Formik számára a form elküldés végpont
     */
    function formikOnSubmit(values: RegistrationValues): void {
        registerUser(values.username, values.email, values.password, values.passwordAgain)
        .then(response => {
            console.log('Registration successful:', response.data)
        })
        .catch(error => {
            console.error('Registration failed:', error)
        })
    }

    return (
        <>
            <div>
                <h1>Registration</h1>
                <p>Here you can register for an account.</p>
            </div>
                <Formik
                    initialValues={{  username: "", email: "", password: "", passwordAgain: "" }}
                    onSubmit={formikOnSubmit}
                    validate={formikValidate}
                    validateOnChange={false}
                >
                {({values, handleChange, handleSubmit, errors }) => (
                    <form className="form" onSubmit={handleSubmit }>
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
                            <label htmlFor="email">Email:</label>
                            <input
                                type="text"
                                id="email"
                                name="email"
                                value={values.email}
                                className="form-control"
                                onChange={handleChange}
                            />
                            {errors.email ? <div>{errors.email}</div> : null}
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
                        <div className="form-group">
                            <label htmlFor="passwordAgain">Confirm Password:</label>
                            <input
                                type="password"
                                id="passwordAgain"
                                name="passwordAgain"
                                value={values.passwordAgain}
                                className="form-control"
                                onChange={handleChange}
                            />
                            {errors.passwordAgain ? <div>{errors.passwordAgain}</div> : null}
                        </div>
                        <button type="submit" className="btn btn-primary">Register</button>
                    </form>
                )}
            </Formik>
        </>
    )
}

export default RegistrationComponents