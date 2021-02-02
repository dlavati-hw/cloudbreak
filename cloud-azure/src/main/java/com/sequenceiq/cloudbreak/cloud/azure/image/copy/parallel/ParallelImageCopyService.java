package com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

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

    private static final long CHUNK_SIZE = 4194304L;

    private static final int BLOB_LEASE_DURATION_SECONDS = 60;

    private final Duration IMAGE_COPY_TIMEOUT = Duration.of(30, ChronoUnit.MINUTES);

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
                .timeout(IMAGE_COPY_TIMEOUT)
                .subscribe();
    }

    private Mono<Void> copyImageChunks(String sourceBlob, long chunkSize, PageBlobAsyncClient pageBlobAsyncClient, BlobLeaseAsyncClient blobLeaseAsyncClient, ImageCopyContext imageCopyContext, String leaseId) {
        ChunkCalculator chunkCalculator = new ChunkCalculator(imageCopyContext.getFilesize(), chunkSize);
        PageRangeCalculator pageRangeCalculator = new PageRangeCalculator();
        List<PageRange> pageRanges = pageRangeCalculator.getAll(chunkCalculator);
        PageBlobRequestConditions pageBlobRequestConditions = new PageBlobRequestConditions().setLeaseId(leaseId);

        return Mono.just(new CopyTasks(pageBlobAsyncClient, sourceBlob, pageBlobRequestConditions))
                .flatMap(copyTasks -> copyTasks.createAll(pageRanges, blobLeaseAsyncClient))
                .doOnError(e -> System.out.println("Final error: " + e))
                .doFinally(s -> System.out.println("Finally called, signal type:" + s));
    }

    private Mono<String> startLeaseImage(BlobLeaseAsyncClient blobLeaseAsyncClient) {
        return blobLeaseAsyncClient.acquireLease(BLOB_LEASE_DURATION_SECONDS);
    }

    private Mono<Response<PageBlobItem>> createImage(PageBlobAsyncClient pageBlobAsyncClient, ImageCopyContext imageCopyContext, Response<BlobProperties> blobPropertiesResponse) {
        long fileSize = blobPropertiesResponse.getValue().getBlobSize();
        imageCopyContext.setFilesize(fileSize);
        return pageBlobAsyncClient.createWithResponse(fileSize, 0L, null, null, null);
    }

    private Mono<Response<BlobProperties>> getPublicBlobPropertiesAsync(Mono<String> sourceBlobUrl) {
        return sourceBlobUrl.flatMap(this::getPublicBlobProperties)
                .doOnSuccess(resp -> {
                    if (resp.getStatusCode() == 503) {
                        throw new RuntimeException("Return code is 503");
                    }
                })
                .retryBackoff(10, Duration.of(1, ChronoUnit.SECONDS), Duration.of(10, ChronoUnit.SECONDS));
    }

    private Mono<Response<BlobProperties>> getPublicBlobProperties(String sourceBlobUrl) {
        BlobAsyncClient blobClient = new BlobClientBuilder().endpoint(sourceBlobUrl).buildAsyncClient();
        return blobClient.getPropertiesWithResponse(null);
    }

}
