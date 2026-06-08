package com.example.domain.service

import com.example.data.repository.BookingRepository
import com.example.data.repository.SeatRepository
import com.example.domain.error.BadRequestException
import com.example.domain.error.ConflictException
import com.example.domain.error.NotFoundException
import com.example.domain.model.Booking
import com.example.domain.model.BookingStatus
import com.example.domain.validation.BookingValidator
import com.example.presentation.dto.CreateBookingRequest
import java.time.Duration
import java.time.LocalDateTime

class BookingService(
    private val bookingRepository: BookingRepository,
    private val seatRepository: SeatRepository
) {
    fun createBooking(userId: Int, request: CreateBookingRequest): Booking {
        val (start, end) = BookingValidator.parseBookingPeriod(request.startTime, request.endTime)

        val seat = seatRepository.findActiveById(request.seatId)
            ?: throw NotFoundException("Место не найдено")

        if (bookingRepository.hasConflict(request.seatId, start, end)) {
            throw ConflictException("Место уже занято в это время")
        }

        val hours = Duration.between(start, end).toMinutes() / 60.0
        val totalPrice = seat.pricePerHour * hours

        return bookingRepository.create(
            userId = userId,
            seatId = request.seatId,
            seatName = seat.name,
            start = start,
            end = end,
            totalPrice = totalPrice
        )
    }

    fun findUserBookings(userId: Int): List<Booking> =
        bookingRepository.findByUser(userId)

    fun findAllBookings(): List<Booking> =
        bookingRepository.findAll()

    fun cancelBooking(bookingId: Int, userId: Int, role: String) {
        val booking = bookingRepository.findByIdForUser(bookingId, userId, role)
            ?: throw NotFoundException("Бронь не найдена")

        if (role != ADMIN_ROLE && booking.status != BookingStatus.Cancelled.value) {
            val startTime = LocalDateTime.parse(booking.startTime)
            val cancelDeadline = startTime.minusHours(CANCEL_LIMIT_HOURS)

            if (cancelDeadline.isBefore(LocalDateTime.now())) {
                throw BadRequestException("Бронь можно отменить не позднее чем за 4 часа до начала")
            }
        }

        val cancelled = bookingRepository.cancel(bookingId, userId, role)
        if (!cancelled) {
            throw NotFoundException("Бронь не найдена")
        }
    }

    private companion object {
        const val ADMIN_ROLE = "admin"
        const val CANCEL_LIMIT_HOURS = 4L
    }
}
