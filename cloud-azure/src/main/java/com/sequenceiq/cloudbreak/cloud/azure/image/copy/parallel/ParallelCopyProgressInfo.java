package com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel;

import java.util.Map;
import java.util.Optional;

public class ParallelCopyProgressInfo {

    private static final String FIELD_PROGRESS = "progress";

    private static final String FIELD_STATUS = "status";

    private static final String FIELD_MESSAGE = "message";

    private static final String FIELD_TIMESTAMP = "timestamp";

    private static final String FIELD_PARALLEL_INFO_PRESENT = "parallelCopyProgressInfoPresent";

    private final String progress;

    private final CopyStatus status;

    private final String message;

    private final String timestamp;

    private ParallelCopyProgressInfo(long copiedChunks, long totalChunks, CopyStatus status, String message, long timestamp) {
        this.progress = String.format("%.2f %%", copiedChunks / (double) totalChunks * 100);
        this.status = status;
        this.message = message;
        this.timestamp = String.format("%d", timestamp);
    }

    private ParallelCopyProgressInfo(String copyProgress, CopyStatus status, String message, String timestamp) {
        this.progress = copyProgress;
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }

    public static ParallelCopyProgressInfo inProgress(long copied, long total, long timestamp) {
        return new ParallelCopyProgressInfo(copied, total, CopyStatus.IN_PROGRESS, "", timestamp);
    }

    public static ParallelCopyProgressInfo finished(int total, long timestamp) {
        return new ParallelCopyProgressInfo(total, total, CopyStatus.FINISHED, "", timestamp);

    }

    public static ParallelCopyProgressInfo failed(long copied, int total, String failureReason, long timestamp) {
        return new ParallelCopyProgressInfo(copied, total, CopyStatus.FAILED, failureReason, timestamp);
    }

    public static Optional<ParallelCopyProgressInfo> fromMap(Map<String, String> blobMetadata) {
        if (blobMetadata == null || !blobMetadata.containsKey(FIELD_PARALLEL_INFO_PRESENT)) {
            return Optional.empty();
        }

        CopyStatus copyStatus = parseCopyStatus(blobMetadata);
        return Optional.of(new ParallelCopyProgressInfo(blobMetadata.get(FIELD_PROGRESS), copyStatus, blobMetadata.get(FIELD_MESSAGE), blobMetadata.get(FIELD_TIMESTAMP)));

    }

    public Map<String, String> toMap() {
        return Map.of(
                FIELD_PROGRESS, progress,
                FIELD_STATUS, status.toString(),
                FIELD_MESSAGE, message,
                FIELD_TIMESTAMP, timestamp,
                FIELD_PARALLEL_INFO_PRESENT, "true"
        );
    }

    public CopyStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public enum CopyStatus {
        IN_PROGRESS,
        FINISHED,
        FAILED,
        UNKNOWN
    }

    private static CopyStatus parseCopyStatus(Map<String, String> blobMetadata) {
        CopyStatus copyStatus;
        try{
            copyStatus = CopyStatus.valueOf(blobMetadata.get(FIELD_STATUS));
        } catch( IllegalArgumentException e) {
            copyStatus = CopyStatus.UNKNOWN;
        }
        return copyStatus;
    }

}
