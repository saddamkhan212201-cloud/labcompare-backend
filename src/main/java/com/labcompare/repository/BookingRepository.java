package com.labcompare.repository;

import com.labcompare.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingRef(String bookingRef);
    List<Booking> findByPhone(String phone);
    List<Booking> findByLabId(Long labId);
}
