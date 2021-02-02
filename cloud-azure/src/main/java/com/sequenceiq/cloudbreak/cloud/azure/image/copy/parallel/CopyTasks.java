package com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.core.http.rest.Response;
import com.azure.storage.blob.models.PageBlobItem;
import com.azure.storage.blob.models.PageBlobRequestConditions;
import com.azure.storage.blob.models.PageRange;
import com.azure.storage.blob.specialized.BlobLeaseAsyncClient;
import com.azure.storage.blob.specialized.PageBlobAsyncClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CopyTasks {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyTasks.class);

    private final PageBlobAsyncClient destinationClient;

    private final String sourceBlobUrl;

    private final PageBlobRequestConditions destinationRequestConditions;

    @Inject
    private ParallelImageCopyParametersService imageCopyParameters;

    @Inject
    private ResponseCodeHandlerService responseCodeHandlerService;


    // TODO: no contructor, but put params into a class and pass that along
    public CopyTasks(PageBlobAsyncClient destinationClient, String sourceBlobUrl, PageBlobRequestConditions destinationRequestConditions) {
        this.destinationClient = destinationClient;
        this.sourceBlobUrl = sourceBlobUrl;
        this.destinationRequestConditions = destinationRequestConditions;
    }

    // TODO: these params do not need to come from outside, a simple filesize might be enough. Refactor the code for better SRP
    public Mono<Void> createAll(List<PageRange> copyRanges, BlobLeaseAsyncClient blobLeaseAsyncClient) {

        AtomicLong successCounter = new AtomicLong();
        AtomicBoolean imageCopyRunning = new AtomicBoolean(true);
        return Flux.just(getLeaseRenewFlux(blobLeaseAsyncClient, imageCopyRunning), getCopyFlux(copyRanges, successCounter, imageCopyRunning))
            .flatMap(x -> x, 2, 2)
            .then();
    }

    private Mono<Void> getLeaseRenewFlux(BlobLeaseAsyncClient blobLeaseAsyncClient, AtomicBoolean copyRunning) {
        return Mono.just(blobLeaseAsyncClient)
                .delayElement(imageCopyParameters.getBlobLeaseRenewInterval())
                .flatMap(client -> client.renewLeaseWithResponse(null))
                .doOnSuccess(resp -> responseCodeHandlerService.handleResponse(resp.getStatusCode()))
                .retryBackoff(imageCopyParameters.getRetryCount(), imageCopyParameters.getBackoffMinimum(), imageCopyParameters.getBackoffMaximum())
                .repeat(copyRunning::get)
                .then(Mono.just(blobLeaseAsyncClient))
                .flatMap(x -> blobLeaseAsyncClient.releaseLeaseWithResponse(destinationRequestConditions))
                .doOnSuccess(resp -> responseCodeHandlerService.handleResponse(resp.getStatusCode()))
                .retryBackoff(imageCopyParameters.getRetryCount(), imageCopyParameters.getBackoffMinimum(), imageCopyParameters.getBackoffMaximum())
            .then();
    }

    private Mono<Void> getCopyFlux(List<PageRange> copyRanges, AtomicLong successCounter, AtomicBoolean imageCopyRunning) {
        return Flux.fromIterable(copyRanges)
                .flatMap(r -> {
                        Mono<Void> response = Mono.just(r)
                                .flatMap(this::sendToServer)
                                .doOnSuccess(resp -> responseCodeHandlerService.handleResponse(resp.getStatusCode()))
                                .retryBackoff(imageCopyParameters.getRetryCount(), imageCopyParameters.getBackoffMinimum(), imageCopyParameters.getBackoffMaximum())
                            .flatMap(copyResp -> {
                                Long copiedCounter = successCounter.incrementAndGet();
                                if (copiedCounter % 100 == 0 || copiedCounter == copyRanges.size()) {
                                    return sendProgressInfo(copiedCounter / (double) copyRanges.size() * 100);
                                }
                                return Mono.empty();
                            })
                                .then();

                            return response;
                        }
                        , imageCopyParameters.getConcurrency(), imageCopyParameters.getPrefetch())
                .doOnComplete(() -> LOGGER.info("Image copy completed successfully"))
                .doFinally(s -> imageCopyRunning.set(false))
                .then();
    }

    private Mono<Response<Void>> sendProgressInfo(Double percentageReady) {
        return Mono.just(percentageReady)
            .flatMap(p -> destinationClient.setMetadataWithResponse(Map.of("copyProgress", String.format("%.2f", percentageReady)), destinationRequestConditions))
                .doOnSuccess(resp -> responseCodeHandlerService.handleResponse(resp.getStatusCode()))
                .retryBackoff(imageCopyParameters.getRetryCount(), imageCopyParameters.getBackoffMinimum(), imageCopyParameters.getBackoffMaximum());
    }

    private Mono<Response<PageBlobItem>> sendToServer(PageRange r) {
        return destinationClient.uploadPagesFromUrlWithResponse(
            r,
            sourceBlobUrl,
            r.getStart(),
            null,
            destinationRequestConditions,
            null
        );
    }
}

