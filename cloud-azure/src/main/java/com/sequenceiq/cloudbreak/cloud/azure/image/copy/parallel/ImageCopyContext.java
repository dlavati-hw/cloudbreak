package com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel;

public class ImageCopyContext {
    private Long filesize;

    private String leaseId;

    public Long getFilesize() {
        return filesize;
    }

    public void setFilesize(Long filesize) {
        this.filesize = filesize;
    }

    public String getLeaseId() {
        return leaseId;
    }

    public void setLeaseId(String leaseId) {
        this.leaseId = leaseId;
    }
}
