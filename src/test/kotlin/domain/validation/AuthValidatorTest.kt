package domain.validation

import com.example.domain.error.BadRequestException
import com.example.domain.validation.AuthValidator
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AuthValidatorTest {

    @Test
    fun `valid register data passes validation`() {
        AuthValidator.validateRegister(
            email = "student@example.com",
            password = "password1",
            name = "Иван"
        )
    }

    @Test
    fun `empty email fails validation`() {
        assertFailsWith<BadRequestException> {
            AuthValidator.validateRegister(
                email = "",
                password = "password1",
                name = "Иван"
            )
        }
    }

    @Test
    fun `invalid email fails validation`() {
        assertFailsWith<BadRequestException> {
            AuthValidator.validateRegister(
                email = "wrong-email",
                password = "password1",
                name = "Иван"
            )
        }
    }

    @Test
    fun `short password fails validation`() {
        assertFailsWith<BadRequestException> {
            AuthValidator.validateRegister(
                email = "student@example.com",
                password = "a1",
                name = "Иван"
            )
        }
    }

    @Test
    fun `password without digits fails validation`() {
        assertFailsWith<BadRequestException> {
            AuthValidator.validateRegister(
                email = "student@example.com",
                password = "password",
                name = "Иван"
            )
        }
    }

    @Test
    fun `valid login data passes validation`() {
        AuthValidator.validateLogin(
            email = "student@example.com",
            password = "password1"
        )
    }

    @Test
    fun `empty login password fails validation`() {
        assertFailsWith<BadRequestException> {
            AuthValidator.validateLogin(
                email = "student@example.com",
                password = ""
            )
        }
    }
}
