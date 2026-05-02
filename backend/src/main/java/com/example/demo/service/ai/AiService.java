package com.example.demo.service.ai;

import com.example.demo.dto.ai.TriageRequest;
import com.example.demo.dto.ai.TriageResponse;
import com.example.demo.dto.ai.VisitSummaryRequest;
import com.example.demo.dto.ai.VisitSummaryResponse;
import com.example.demo.entity.Appointment;
import com.example.demo.entity.Department;
import com.example.demo.entity.MedicalRecord;
import com.example.demo.entity.Prescription;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DepartmentRepository;
import com.example.demo.repository.MedicalRecordRepository;
import com.example.demo.repository.PrescriptionRepository;
import com.example.demo.service.MedicalRecordService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class AiService {

    private static final String DISCLAIMER =
            "This is an AI-generated suggestion based only on the description provided. "
                    + "It is not a medical diagnosis. If symptoms are severe, worsening, or include red flags, "
                    + "please go to the emergency room or call your local emergency number.";

    private final GroqClient groq;
    private final DepartmentRepository departmentRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicalRecordService medicalRecordService;

    public AiService(
            GroqClient groq,
            DepartmentRepository departmentRepository,
            AppointmentRepository appointmentRepository,
            MedicalRecordRepository medicalRecordRepository,
            PrescriptionRepository prescriptionRepository,
            MedicalRecordService medicalRecordService
    ) {
        this.groq = groq;
        this.departmentRepository = departmentRepository;
        this.appointmentRepository = appointmentRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.medicalRecordService = medicalRecordService;
    }

    public boolean isEnabled() {
        return groq.isEnabled();
    }

    /* ------------------------------------------------------------------ */
    /*  TRIAGE                                                              */
    /* ------------------------------------------------------------------ */

    public TriageResponse triage(TriageRequest req) {
        List<Department> departments = departmentRepository.findAll();
        String departmentList = departments.stream()
                .map(Department::getName)
                .collect(Collectors.joining(", "));

        String system = """
                You are a triage assistant for a hospital booking system. You DO NOT provide a diagnosis.
                Your only job is to (1) suggest the most appropriate department from the allowed list,
                (2) estimate the urgency, (3) draft a one-line chief complaint the doctor will see,
                and (4) flag any red-flag symptoms that suggest the patient should go to the ER instead of booking.

                Allowed departments: %s
                If none clearly fits, suggest "General Medicine".

                INPUT VALIDATION (very important):
                If the input is gibberish, random characters, empty, a single random word, profanity,
                or has nothing to do with health (e.g. "fdbedfberb", "asdfasdf", "hello", "test",
                "what is 2+2", song lyrics, marketing copy), DO NOT make a triage suggestion.
                Instead return EXACTLY this JSON:
                {
                  "suggestedDepartment": "__INVALID__",
                  "urgency": "LOW",
                  "draftChiefComplaint": "Please describe what you're feeling — for example, where it hurts, how long it's been going on, and any other symptoms.",
                  "redFlags": []
                }

                Urgency rules (only when input is medically coherent):
                  - URGENT: severe pain, trouble breathing, chest pain with sweating/radiation, confusion,
                            sudden weakness, heavy bleeding, suspected stroke or heart attack signs.
                  - NORMAL: notable symptoms persisting > 48h, fever, infection signs, conditions worsening.
                  - LOW: mild, intermittent, or routine concerns.

                Return ONLY a JSON object with this exact shape (no prose, no markdown):
                {
                  "suggestedDepartment": "<one of the allowed names, or __INVALID__ if input is not a real symptom description>",
                  "urgency": "LOW" | "NORMAL" | "URGENT",
                  "draftChiefComplaint": "<<= 200 chars, third person, e.g. 'Reports chest tightness for 5 days, no SOB'>",
                  "redFlags": ["..."]
                }
                """.formatted(departmentList);

        String user = """
                Patient symptoms: %s
                Age: %s
                Gender: %s
                """.formatted(
                        req.getSymptoms(),
                        req.getAgeYears() == null ? "unknown" : req.getAgeYears().toString(),
                        req.getGender() == null ? "unknown" : req.getGender()
        );

        JsonNode json = groq.completeJson(List.of(
                GroqClient.Message.system(system),
                GroqClient.Message.user(user)
        ), 0.2);

        String departmentName = json.path("suggestedDepartment").asText("General Medicine");
        Long departmentId = departments.stream()
                .filter(d -> d.getName().equalsIgnoreCase(departmentName))
                .findFirst()
                .map(Department::getId)
                .orElse(null);

        TriageResponse.Urgency urgency;
        try {
            urgency = TriageResponse.Urgency.valueOf(
                    json.path("urgency").asText("NORMAL").toUpperCase());
        } catch (IllegalArgumentException ex) {
            urgency = TriageResponse.Urgency.NORMAL;
        }

        List<String> redFlags = new ArrayList<>();
        JsonNode rf = json.path("redFlags");
        if (rf.isArray()) {
            StreamSupport.stream(rf.spliterator(), false)
                    .map(JsonNode::asText)
                    .filter(s -> s != null && !s.isBlank())
                    .forEach(redFlags::add);
        }

        return TriageResponse.builder()
                .suggestedDepartment(departmentName)
                .suggestedDepartmentId(departmentId)
                .urgency(urgency)
                .draftChiefComplaint(json.path("draftChiefComplaint").asText(""))
                .redFlags(redFlags)
                .disclaimer(DISCLAIMER)
                .build();
    }

    /* ------------------------------------------------------------------ */
    /*  VISIT SUMMARY                                                       */
    /* ------------------------------------------------------------------ */

    @Transactional
    public VisitSummaryResponse visitSummary(VisitSummaryRequest req) {
        Appointment appt = appointmentRepository.findById(req.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        MedicalRecord record = medicalRecordRepository.findByAppointmentId(req.getAppointmentId())
                .orElse(null);

        String chiefComplaint = record == null ? "" : nz(record.getChiefComplaint());
        String diagnosis      = record == null ? "" : nz(record.getDiagnosis());
        String plan           = record == null ? "" : nz(record.getPlan());

        List<Prescription> rxs = prescriptionRepository.findByAppointmentIdOrderByCreatedAtDesc(req.getAppointmentId());
        String rxList = rxs.stream()
                .filter(rx -> rx.getStatus() != Prescription.Status.CANCELLED)
                .flatMap(rx -> rx.getItems().stream())
                .map(i -> "- " + i.getDrugName()
                        + (i.getDose() == null ? "" : " " + i.getDose())
                        + (i.getFrequency() == null ? "" : ", " + i.getFrequency())
                        + (i.getDurationDays() == null ? "" : " for " + i.getDurationDays() + " days"))
                .collect(Collectors.joining("\n"));
        if (rxList.isBlank()) rxList = "(none)";

        String system = """
                You are a clinical writer. Convert the doctor's notes into a brief, plain-language
                summary the patient will read in their app.
                Rules:
                  - 80–140 words.
                  - Second person ("you").
                  - Use simple language; avoid Latin/medical jargon when possible (translate it if needed).
                  - Do not introduce information that is not in the notes. If a section is empty, skip it.
                  - End with one short reassuring sentence about reaching out if anything changes.
                  - Plain text only — no markdown, no headers, no bullet lists.
                """;

        String user = """
                Doctor's notes for an appointment titled "%s":

                Chief complaint: %s
                Diagnosis: %s
                Plan: %s
                Prescriptions:
                %s
                """.formatted(appt.getTitle(), chiefComplaint, diagnosis, plan, rxList);

        String summary = groq.complete(List.of(
                GroqClient.Message.system(system),
                GroqClient.Message.user(user)
        ), 0.4, false).trim();

        boolean saved = false;
        if (req.isSave()) {
            medicalRecordService.setPatientSummary(req.getAppointmentId(), summary);
            saved = true;
        }
        return VisitSummaryResponse.builder().summary(summary).saved(saved).build();
    }

    private String nz(String s) { return s == null ? "" : s; }
}
