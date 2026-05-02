package com.example.demo.config;

import com.example.demo.entity.Announcement;
import com.example.demo.entity.ApprovalStatus;
import com.example.demo.entity.Department;
import com.example.demo.entity.DoctorAvailability;
import com.example.demo.entity.DoctorProfile;
import com.example.demo.entity.PatientProfile;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.AnnouncementRepository;
import com.example.demo.repository.DepartmentRepository;
import com.example.demo.repository.DoctorAvailabilityRepository;
import com.example.demo.repository.DoctorProfileRepository;
import com.example.demo.repository.PatientProfileRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initData(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            DoctorAvailabilityRepository availabilityRepository,
            DepartmentRepository departmentRepository,
            DoctorProfileRepository doctorProfileRepository,
            PatientProfileRepository patientProfileRepository,
            AnnouncementRepository announcementRepository
    ) {
        return args -> {
            Map<String, String> roles = Map.of(
                    "ROLE_USER", "Patient role",
                    "ROLE_DOCTOR", "Doctor role",
                    "ROLE_NURSE", "Nurse role",
                    "ROLE_ADMIN", "Administrator role"
            );

            roles.forEach((name, description) -> {
                if (roleRepository.findByName(name).isEmpty()) {
                    roleRepository.save(Role.builder()
                            .name(name)
                            .description(description)
                            .build());
                }
            });

            createUserIfMissing(userRepository, roleRepository, passwordEncoder,
                    "admin@example.com", "Admin", "User", "admin123", "ROLE_ADMIN");
            createUserIfMissing(userRepository, roleRepository, passwordEncoder,
                    "doctor@example.com", "Emily", "Stone", "password123", "ROLE_DOCTOR");
            createUserIfMissing(userRepository, roleRepository, passwordEncoder,
                    "nurse@example.com", "Nina", "Brooks", "password123", "ROLE_NURSE");
            createUserIfMissing(userRepository, roleRepository, passwordEncoder,
                    "john@example.com", "John", "Doe", "password123", "ROLE_USER");

            backfillApprovalStatus(userRepository);

            seedDoctorAvailability(userRepository, availabilityRepository);
            seedDepartments(departmentRepository);
            seedDoctorProfile(userRepository, doctorProfileRepository, departmentRepository);
            seedPatientProfile(userRepository, patientProfileRepository);
            seedAnnouncement(announcementRepository);

            log.info("");
            log.info("====================================================");
            log.info("  Lumen Health · demo accounts ready to log in:");
            log.info("    Admin   admin@example.com   admin123");
            log.info("    Patient john@example.com    password123");
            log.info("    Doctor  doctor@example.com  password123");
            log.info("    Nurse   nurse@example.com   password123");
            log.info("====================================================");
            log.info("");
        };
    }

    private void backfillApprovalStatus(UserRepository userRepository) {
        userRepository.findAll().forEach(user -> {
            if (user.getApprovalStatus() == null) {
                user.setApprovalStatus(ApprovalStatus.APPROVED);
                userRepository.save(user);
            }
        });
    }

    private void seedDoctorAvailability(UserRepository userRepository, DoctorAvailabilityRepository availabilityRepository) {
        userRepository.findByEmailIgnoreCase("doctor@example.com").ifPresent(doctor -> {
            if (!availabilityRepository.findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(doctor.getId()).isEmpty()) {
                return;
            }
            List<DayOfWeek> weekdays = List.of(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            );
            for (DayOfWeek day : weekdays) {
                availabilityRepository.save(DoctorAvailability.builder()
                        .doctor(doctor).dayOfWeek(day)
                        .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0))
                        .slotDurationMinutes(30).active(true).build());
                availabilityRepository.save(DoctorAvailability.builder()
                        .doctor(doctor).dayOfWeek(day)
                        .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(17, 0))
                        .slotDurationMinutes(30).active(true).build());
            }
        });
    }

    private void seedDepartments(DepartmentRepository repo) {
        if (repo.count() > 0) return;
        repo.save(Department.builder().name("Cardiology")
                .description("Heart and vascular conditions.")
                .color("#0F4C5C").icon("heart").build());
        repo.save(Department.builder().name("Pediatrics")
                .description("Care for infants, children, and adolescents.")
                .color("#7BAE7F").icon("baby").build());
        repo.save(Department.builder().name("Dermatology")
                .description("Skin, hair, and nail conditions.")
                .color("#E36F6F").icon("sun").build());
        repo.save(Department.builder().name("General Medicine")
                .description("First-line evaluation and primary care.")
                .color("#266B73").icon("stethoscope").build());
        repo.save(Department.builder().name("Orthopedics")
                .description("Musculoskeletal injuries and disorders.")
                .color("#B45309").icon("bone").build());
    }

    private void seedDoctorProfile(
            UserRepository userRepository,
            DoctorProfileRepository profileRepository,
            DepartmentRepository departmentRepository
    ) {
        userRepository.findByEmailIgnoreCase("doctor@example.com").ifPresent(doctor -> {
            if (profileRepository.findByUserId(doctor.getId()).isPresent()) return;
            HashSet<Department> departments = new HashSet<>();
            departmentRepository.findByNameIgnoreCase("Cardiology").ifPresent(departments::add);
            departmentRepository.findByNameIgnoreCase("General Medicine").ifPresent(departments::add);
            profileRepository.save(DoctorProfile.builder()
                    .user(doctor)
                    .departments(departments)
                    .specialty("Cardiology")
                    .licenseNumber("MD-2014-0432")
                    .bio("Cardiologist with a focus on preventive care and arrhythmia management. Practicing since 2014.")
                    .languages("English, French")
                    .consultationFeeCents(8000)
                    .yearsExperience(11)
                    .build());
        });
    }

    private void seedPatientProfile(UserRepository userRepository, PatientProfileRepository profileRepository) {
        userRepository.findByEmailIgnoreCase("john@example.com").ifPresent(patient -> {
            if (profileRepository.findByUserId(patient.getId()).isPresent()) return;
            profileRepository.save(PatientProfile.builder()
                    .user(patient)
                    .dateOfBirth(LocalDate.of(1991, 6, 18))
                    .gender("Male")
                    .bloodType("O+")
                    .allergies("Penicillin")
                    .chronicConditions("Mild seasonal asthma")
                    .emergencyContactName("Jane Doe")
                    .emergencyContactPhone("+1 555 0102")
                    .build());
        });
    }

    private void seedAnnouncement(AnnouncementRepository announcementRepository) {
        if (announcementRepository.count() > 0) return;
        announcementRepository.save(Announcement.builder()
                .title("Welcome to Lumen Health")
                .body("This is the staff portal. New patient sign-ups now require admin approval before access is granted.")
                .audience(Announcement.Audience.STAFF)
                .tone(Announcement.Tone.INFO)
                .active(true)
                .build());
    }

    private void createUserIfMissing(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            String email,
            String firstName,
            String lastName,
            String password,
            String roleName
    ) {
        if (!userRepository.existsByEmailIgnoreCase(email)) {
            Role role = roleRepository.findByName(roleName).orElseThrow();
            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .firstName(firstName)
                    .lastName(lastName)
                    .roles(new HashSet<>(Collections.singletonList(role)))
                    .enabled(true)
                    .approvalStatus(ApprovalStatus.APPROVED)
                    .build();
            userRepository.save(user);
        }
    }
}
