package com.example.demo.service;

import com.example.demo.dto.DoctorAvailabilityDTO;
import com.example.demo.dto.DoctorAvailabilityRequest;
import com.example.demo.dto.TimeSlotDTO;
import com.example.demo.entity.Appointment;
import com.example.demo.entity.AppointmentStatus;
import com.example.demo.entity.DoctorAvailability;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DoctorAvailabilityRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DoctorAvailabilityService {

    private final DoctorAvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final HolidayService holidayService;
    private final TimeOffService timeOffService;

    public DoctorAvailabilityService(
            DoctorAvailabilityRepository availabilityRepository,
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            HolidayService holidayService,
            TimeOffService timeOffService
    ) {
        this.availabilityRepository = availabilityRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.holidayService = holidayService;
        this.timeOffService = timeOffService;
    }

    @Transactional(readOnly = true)
    public List<DoctorAvailabilityDTO> getAvailabilityForDoctor(Long doctorId) {
        return availabilityRepository.findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(doctorId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DoctorAvailabilityDTO> getMyAvailability() {
        User doctor = requireCurrentDoctor();
        return getAvailabilityForDoctor(doctor.getId());
    }

    @Transactional
    public DoctorAvailabilityDTO addMyAvailability(DoctorAvailabilityRequest request) {
        User doctor = requireCurrentDoctor();
        validateRequest(request);
        DoctorAvailability availability = DoctorAvailability.builder()
                .doctor(doctor)
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .slotDurationMinutes(request.getSlotDurationMinutes())
                .active(request.getActive() == null ? Boolean.TRUE : request.getActive())
                .build();
        return toDTO(availabilityRepository.save(availability));
    }

    @Transactional
    public DoctorAvailabilityDTO updateMyAvailability(Long id, DoctorAvailabilityRequest request) {
        User doctor = requireCurrentDoctor();
        validateRequest(request);
        DoctorAvailability availability = availabilityRepository.findByIdAndDoctorId(id, doctor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Availability not found"));
        availability.setDayOfWeek(request.getDayOfWeek());
        availability.setStartTime(request.getStartTime());
        availability.setEndTime(request.getEndTime());
        availability.setSlotDurationMinutes(request.getSlotDurationMinutes());
        if (request.getActive() != null) {
            availability.setActive(request.getActive());
        }
        return toDTO(availabilityRepository.save(availability));
    }

    @Transactional
    public void deleteMyAvailability(Long id) {
        User doctor = requireCurrentDoctor();
        DoctorAvailability availability = availabilityRepository.findByIdAndDoctorId(id, doctor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Availability not found"));
        availabilityRepository.delete(availability);
    }

    @Transactional(readOnly = true)
    public List<TimeSlotDTO> getSlotsForDoctorOnDate(Long doctorId, LocalDate date) {
        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        ensureHasRole(doctor, "ROLE_DOCTOR");

        // Clinic-wide closure (holidays) and per-doctor approved leave both wipe the day.
        if (holidayService.isHoliday(date) || timeOffService.isDoctorOnApprovedLeave(doctorId, date)) {
            return List.of();
        }

        List<DoctorAvailability> windows = availabilityRepository
                .findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, date.getDayOfWeek());

        if (windows.isEmpty()) {
            return List.of();
        }

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        List<Appointment> sameDayAppointments = appointmentRepository
                .findByDoctorIdAndAppointmentDateTimeBetweenAndStatusNot(
                        doctorId, dayStart, dayEnd, AppointmentStatus.CANCELLED
                );

        Set<LocalDateTime> takenStarts = sameDayAppointments.stream()
                .map(Appointment::getAppointmentDateTime)
                .collect(Collectors.toCollection(HashSet::new));

        List<TimeSlotDTO> slots = new ArrayList<>();

        for (DoctorAvailability window : windows) {
            int duration = window.getSlotDurationMinutes() == null ? 30 : window.getSlotDurationMinutes();
            LocalDateTime cursor = LocalDateTime.of(date, window.getStartTime());
            LocalDateTime windowEnd = LocalDateTime.of(date, window.getEndTime());

            while (!cursor.plusMinutes(duration).isAfter(windowEnd)) {
                LocalDateTime slotEnd = cursor.plusMinutes(duration);
                // Available iff no appointment already occupies that slot.
                // The @Future validator on AppointmentRequest still rejects
                // genuinely past bookings — we don't grey them out here so
                // the picker stays clickable on a half-elapsed day.
                boolean available = !takenStarts.contains(cursor);
                slots.add(TimeSlotDTO.builder()
                        .start(cursor)
                        .end(slotEnd)
                        .available(available)
                        .build());
                cursor = slotEnd;
            }
        }

        slots.sort((a, b) -> a.getStart().compareTo(b.getStart()));
        return slots;
    }

    public boolean isValidSlot(Long doctorId, LocalDateTime requestedStart) {
        if (requestedStart == null) {
            return false;
        }
        LocalDate day = requestedStart.toLocalDate();
        if (holidayService.isHoliday(day) || timeOffService.isDoctorOnApprovedLeave(doctorId, day)) {
            return false;
        }
        List<DoctorAvailability> windows = availabilityRepository
                .findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, requestedStart.getDayOfWeek());
        if (windows.isEmpty()) {
            return false;
        }
        LocalTime requestedTime = requestedStart.toLocalTime();
        for (DoctorAvailability window : windows) {
            int duration = window.getSlotDurationMinutes() == null ? 30 : window.getSlotDurationMinutes();
            LocalTime cursor = window.getStartTime();
            while (!cursor.plusMinutes(duration).isAfter(window.getEndTime())) {
                if (cursor.equals(requestedTime)) {
                    return true;
                }
                cursor = cursor.plusMinutes(duration);
            }
        }
        return false;
    }

    public boolean isSlotTaken(Long doctorId, LocalDateTime start, Long excludeAppointmentId) {
        List<Appointment> existing = appointmentRepository
                .findByDoctorIdAndAppointmentDateTimeBetweenAndStatusNot(
                        doctorId, start, start.plusSeconds(1), AppointmentStatus.CANCELLED
                );
        return existing.stream().anyMatch(a ->
                a.getAppointmentDateTime().equals(start)
                        && (excludeAppointmentId == null || !a.getId().equals(excludeAppointmentId))
        );
    }

    private void validateRequest(DoctorAvailabilityRequest request) {
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        long minutes = java.time.Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
        if (minutes < request.getSlotDurationMinutes()) {
            throw new IllegalArgumentException("Window must be at least one slot long");
        }
    }

    private User requireCurrentDoctor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        if (!hasRole(user, "ROLE_DOCTOR")) {
            throw new AccessDeniedException("Only doctors can manage their availability");
        }
        return user;
    }

    private void ensureHasRole(User user, String roleName) {
        if (!hasRole(user, roleName)) {
            throw new IllegalArgumentException("User does not have role " + roleName);
        }
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().map(Role::getName).anyMatch(roleName::equals);
    }

    private DoctorAvailabilityDTO toDTO(DoctorAvailability availability) {
        User doctor = availability.getDoctor();
        return DoctorAvailabilityDTO.builder()
                .id(availability.getId())
                .doctorId(doctor.getId())
                .doctorName(doctor.getFirstName() + " " + doctor.getLastName())
                .dayOfWeek(availability.getDayOfWeek())
                .startTime(availability.getStartTime())
                .endTime(availability.getEndTime())
                .slotDurationMinutes(availability.getSlotDurationMinutes())
                .active(Boolean.TRUE.equals(availability.getActive()))
                .build();
    }
}
