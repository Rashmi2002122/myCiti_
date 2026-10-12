package com.myciti.dto;

import java.util.List;

public class AdminSubmissionsResponse {

    private List<SubmissionResponse> submissions;
    private Counts counts;

    public AdminSubmissionsResponse() {
    }

    public AdminSubmissionsResponse(List<SubmissionResponse> submissions, Counts counts) {
        this.submissions = submissions;
        this.counts = counts;
    }

    public List<SubmissionResponse> getSubmissions() {
        return submissions;
    }

    public void setSubmissions(List<SubmissionResponse> submissions) {
        this.submissions = submissions;
    }

    public Counts getCounts() {
        return counts;
    }

    public void setCounts(Counts counts) {
        this.counts = counts;
    }

    public static class Counts {
        private long pending;
        private long approved;
        private long declined;
        private long total;

        public Counts() {
        }

        public Counts(long pending, long approved, long declined, long total) {
            this.pending = pending;
            this.approved = approved;
            this.declined = declined;
            this.total = total;
        }

        public long getPending() {
            return pending;
        }

        public void setPending(long pending) {
            this.pending = pending;
        }

        public long getApproved() {
            return approved;
        }

        public void setApproved(long approved) {
            this.approved = approved;
        }

        public long getDeclined() {
            return declined;
        }

        public void setDeclined(long declined) {
            this.declined = declined;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }
    }
}
