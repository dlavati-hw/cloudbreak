package com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel;

import static com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel.ParallelImageCopyParametersService.CHUNK_SIZE;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.PageBlobItem;
import com.azure.storage.blob.models.PageBlobRequestConditions;
import com.azure.storage.blob.models.PageRange;
import com.azure.storage.blob.specialized.BlobLeaseAsyncClient;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import com.azure.storage.blob.specialized.PageBlobAsyncClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureImageInfo;
import com.sequenceiq.cloudbreak.cloud.model.Image;

import reactor.core.publisher.Mono;

@Service
public class ParallelImageCopyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelImageCopyService.class);

    @Inject
    private ParallelImageCopyParametersService imageCopyParameters;

    @Inject
    private ResponseCodeHandlerService responseCodeHandlerService;

    public void copyImage(Image image, AzureClient client, String imageStorageName, String imageResourceGroupName, AzureImageInfo azureImageInfo) {

        PageBlobAsyncClient pageBlobAsyncClient
                = client.getPageBlobAsyncClient(imageResourceGroupName, imageStorageName, "images", azureImageInfo.getImageName());
        BlobLeaseAsyncClient blobLeaseAsyncClient = new BlobLeaseClientBuilder().blobAsyncClient(pageBlobAsyncClient).buildAsyncClient();
        String sourceBlob = image.getImageName();

        ImageCopyContext imageCopyContext = new ImageCopyContext();
        getPublicBlobPropertiesAsync(Mono.just(sourceBlob))
                .flatMap(blobPropertiesResponse -> createImage(pageBlobAsyncClient, imageCopyContext, blobPropertiesResponse))
                .flatMap(pageBlobItemResponse -> startLeaseImage(blobLeaseAsyncClient))
                .flatMap(leaseId -> copyImageChunks(sourceBlob, CHUNK_SIZE, pageBlobAsyncClient, blobLeaseAsyncClient, imageCopyContext, leaseId))
                .timeout(imageCopyParameters.getImageCopyTimeout())
                .subscribe();
    }

    private Mono<Void> copyImageChunks(String sourceBlob, long chunkSize, PageBlobAsyncClient pageBlobAsyncClient, BlobLeaseAsyncClient blobLeaseAsyncClient, ImageCopyContext imageCopyContext, String leaseId) {
        ChunkCalculator chunkCalculator = new ChunkCalculator(imageCopyContext.getFilesize(), chunkSize);
        PageRangeCalculator pageRangeCalculator = new PageRangeCalculator();
        List<PageRange> pageRanges = pageRangeCalculator.getAll(chunkCalculator);
        PageBlobRequestConditions pageBlobRequestConditions = new PageBlobRequestConditions().setLeaseId(leaseId);

        return Mono.just(new CopyTasks(pageBlobAsyncClient, sourceBlob, pageBlobRequestConditions))
                .flatMap(copyTasks -> copyTasks.createAll(pageRanges, blobLeaseAsyncClient))
                .doOnError(e -> LOGGER.warn("Final error: "))
                .doFinally(s -> LOGGER.info("Finally called, signal type: {}", s));
    }

    private Mono<String> startLeaseImage(BlobLeaseAsyncClient blobLeaseAsyncClient) {
        return blobLeaseAsyncClient.acquireLease(imageCopyParameters.getBlobLeaseDurationSeconds());
    }

    private Mono<Response<PageBlobItem>> createImage(PageBlobAsyncClient pageBlobAsyncClient, ImageCopyContext imageCopyContext, Response<BlobProperties> blobPropertiesResponse) {
        long fileSize = blobPropertiesResponse.getValue().getBlobSize();
        imageCopyContext.setFilesize(fileSize);
        return pageBlobAsyncClient
                .createWithResponse(fileSize, 0L, null, null, null)
                .doOnSuccess(resp -> responseCodeHandlerService.handleResponse(resp.getStatusCode()))
                .retryBackoff(imageCopyParameters.getRetryCount(), imageCopyParameters.getBackoffMinimum(), imageCopyParameters.getBackoffMaximum());
    }

    private Mono<Response<BlobProperties>> getPublicBlobPropertiesAsync(Mono<String> sourceBlobUrl) {
        return sourceBlobUrl.flatMap(this::getPublicBlobProperties)
                .doOnSuccess(resp -> responseCodeHandlerService.handleResponse(resp.getStatusCode()))
                .retryBackoff(imageCopyParameters.getRetryCount(), imageCopyParameters.getBackoffMinimum(), imageCopyParameters.getBackoffMaximum());
    }

    private Mono<Response<BlobProperties>> getPublicBlobProperties(String sourceBlobUrl) {
        BlobAsyncClient blobClient = new BlobClientBuilder().endpoint(sourceBlobUrl).buildAsyncClient();
        return blobClient.getPropertiesWithResponse(null)
                .doOnSuccess(resp -> responseCodeHandlerService.handleResponse(resp.getStatusCode()))
                .retryBackoff(imageCopyParameters.getRetryCount(), imageCopyParameters.getBackoffMinimum(), imageCopyParameters.getBackoffMaximum());
    }

}
