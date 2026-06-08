package domain.validation

import com.example.domain.error.BadRequestException
import com.example.domain.validation.BookingValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.time.LocalDateTime

class BookingValidatorTest {

    @Test
    fun `valid booking period parses successfully`() {
        val start = LocalDateTime.now().plusDays(1).withNano(0)
        val end = start.plusHours(2)

        val result = BookingValidator.parseBookingPeriod(start.toString(), end.toString())

        assertEquals(start, result.first)
        assertEquals(end, result.second)
    }

    @Test
    fun `end before start fails validation`() {
        val start = LocalDateTime.now().plusDays(1).withNano(0)
        val end = start.minusHours(1)

        assertFailsWith<BadRequestException> {
            BookingValidator.parseBookingPeriod(start.toString(), end.toString())
        }
    }

    @Test
    fun `booking in past fails validation`() {
        val start = LocalDateTime.now().minusHours(2).withNano(0)
        val end = start.plusHours(1)

        assertFailsWith<BadRequestException> {
            BookingValidator.parseBookingPeriod(start.toString(), end.toString())
        }
    }

    @Test
    fun `invalid date format fails validation`() {
        assertFailsWith<BadRequestException> {
            BookingValidator.parseBookingPeriod("wrong-date", "2026-06-10T12:00:00")
        }
    }
}
