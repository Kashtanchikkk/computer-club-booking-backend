package com.example.domain.service

import com.example.data.repository.BookingRepository
import com.example.data.repository.SeatRepository
import com.example.domain.error.ConflictException
import com.example.domain.error.NotFoundException
import com.example.domain.model.Booking
import com.example.domain.validation.BookingValidator
import com.example.presentation.dto.CreateBookingRequest
import java.time.Duration

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
        val cancelled = bookingRepository.cancel(bookingId, userId, role)
        if (!cancelled) {
            throw NotFoundException("Бронь не найдена")
        }
    }
}
