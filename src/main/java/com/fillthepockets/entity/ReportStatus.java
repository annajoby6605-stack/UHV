package com.fillthepockets.entity;

public enum ReportStatus {
    ACCEPTED,
    EMPLOYEE_APPROVED,
    REJECTED_UNRELATED,
    REJECTED_DUPLICATE,
    NEEDS_REVIEW,
    PENDING_ADMIN_APPROVAL;

    public static ReportStatus fromLegacyValue(String value) {
        if (value == null) {
            return null;
        }

        switch (value.trim()) {
            case "EMPLOYEE_APPROVED":
                return ACCEPTED;
            case "PENDING_ADMIN_APPROVAL":
                return NEEDS_REVIEW;
            default:
                return ReportStatus.valueOf(value);
        }
    }
}
