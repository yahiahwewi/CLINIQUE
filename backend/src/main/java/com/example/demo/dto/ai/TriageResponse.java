package com.example.demo.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriageResponse {
    public enum Urgency { LOW, NORMAL, URGENT }

    /** Suggested department name (must match an existing Department, or null). */
    private String suggestedDepartment;

    private Long suggestedDepartmentId;

    private Urgency urgency;

    /** A short chief complaint the patient can paste into the booking form. */
    private String draftChiefComplaint;

    /** Symptoms suggesting emergency care (empty if none). */
    private List<String> redFlags;

    private String disclaimer;
}
