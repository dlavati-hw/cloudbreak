package com.sequenceiq.cloudbreak.cloud.azure.image.copy;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureImageInfo;
import com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel.ParallelImageCopyService;
import com.sequenceiq.cloudbreak.cloud.azure.image.copy.sequential.SequentialImageCopyService;
import com.sequenceiq.cloudbreak.cloud.model.Image;

@Service
public class ImageCopyService {

    private boolean isAzureImageCopyParallelEnabled = true;

    @Inject
    private ParallelImageCopyService parallelImageCopyService;

    @Inject
    private SequentialImageCopyService sequentialImageCopyService;

    public void copyImage(Image image, AzureClient client, String imageStorageName, String imageResourceGroupName, AzureImageInfo azureImageInfo) {
        if (isAzureImageCopyParallelEnabled) {
            parallelImageCopyService.copyImage(image, client, imageStorageName, imageResourceGroupName, azureImageInfo);
        } else {
            sequentialImageCopyService.copyImage(image, client, imageStorageName, imageResourceGroupName, azureImageInfo);
        }
    }
}
