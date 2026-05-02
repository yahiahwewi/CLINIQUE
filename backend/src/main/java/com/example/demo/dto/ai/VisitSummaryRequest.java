package com.example.demo.dto.ai;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitSummaryRequest {
    @NotNull
    private Long appointmentId;

    /** If true, the generated summary is saved to MedicalRecord.patientSummary
     *  and a notification is sent to the patient. */
    @Builder.Default
    private boolean save = false;
}
