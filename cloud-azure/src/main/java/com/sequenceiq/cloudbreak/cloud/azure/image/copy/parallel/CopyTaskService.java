package com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.core.http.rest.Response;
import com.azure.storage.blob.models.PageBlobItem;
import com.azure.storage.blob.models.PageRange;
import com.azure.storage.blob.specialized.BlobLeaseAsyncClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CopyTaskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyTaskService.class);

    @Inject
    private ParallelImageCopyParametersService imageCopyParameters;

    @Inject
    private ResponseCodeHandlerService responseCodeHandlerService;

    public Mono<Void> createAll(CopyTaskParameters copyTaskParameters) {

        AtomicLong successCounter = new AtomicLong();
        AtomicBoolean imageCopyRunning = new AtomicBoolean(true);
        return Flux.just(
                getLeaseRenewFlux(copyTaskParameters, imageCopyRunning),
                getCopyFlux(copyTaskParameters, successCounter, imageCopyRunning))
                .flatMap(x -> x, 2, 2)
                .then();
    }

    private Mono<Void> getLeaseRenewFlux(CopyTaskParameters copyTaskParameters, AtomicBoolean imageCopyRunning) {
        BlobLeaseAsyncClient blobLeaseAsyncClient = copyTaskParameters.getBlobLeaseAsyncClient();
        return Mono.just(blobLeaseAsyncClient)
                .delayElement(imageCopyParameters.getBlobLeaseRenewInterval())
                .flatMap(client -> client.renewLeaseWithResponse(null))
                .doOnSuccess(resp -> responseCodeHandlerService.handleResponse(resp.getStatusCode()))
                .retryBackoff(imageCopyParameters.getRetryCount(), imageCopyParameters.getBackoffMinimum(), imageCopyParameters.getBackoffMaximum())
                .repeat(imageCopyRunning::get)
                .then(Mono.just(blobLeaseAsyncClient))
                .flatMap(x -> blobLeaseAsyncClient.releaseLeaseWithResponse(copyTaskParameters.getDestinationRequestConditions()))
                .doOnSuccess(resp -> responseCodeHandlerService.handleResponse(resp.getStatusCode()))
                .retryBackoff(imageCopyParameters.getRetryCount(), imageCopyParameters.getBackoffMinimum(), imageCopyParameters.getBackoffMaximum())
                .then();
    }

    private Mono<Void> getCopyFlux(CopyTaskParameters copyTaskParameters, AtomicLong successCounter, AtomicBoolean imageCopyRunning) {
        return Flux.fromIterable(copyTaskParameters.getCopyRanges())
                .flatMap(r -> Mono.just(r)
                                .flatMap(pageRange -> sendToServer(copyTaskParameters, pageRange))
                                .doOnSuccess(resp -> responseCodeHandlerService.handleResponse(resp.getStatusCode()))
                                .retryBackoff(imageCopyParameters.getRetryCount(), imageCopyParameters.getBackoffMinimum(), imageCopyParameters.getBackoffMaximum())
                                .flatMap(copyResp -> calculateProgress(copyTaskParameters, successCounter))
                                .then()
                        , imageCopyParameters.getConcurrency(), imageCopyParameters.getPrefetch())
                .doOnComplete(() -> LOGGER.info("Image copy completed successfully"))
                .doFinally(s -> imageCopyRunning.set(false))
                .then();
    }

    @NotNull
    private Mono<Response<Void>> calculateProgress(CopyTaskParameters copyTaskParameters, AtomicLong successCounter) {
        Long copiedCounter = successCounter.incrementAndGet();
        long totalTasks = copyTaskParameters.getCopyRanges().size();
        if (copiedCounter % 100 == 0 || copiedCounter == totalTasks) {
            return sendProgressInfo(copyTaskParameters, copiedCounter / (double) totalTasks * 100);
        }
        return Mono.empty();
    }

    private Mono<Response<Void>> sendProgressInfo(CopyTaskParameters copyTaskParameters, Double percentageReady) {
        return Mono.just(percentageReady)
                .flatMap(p -> copyTaskParameters.getPageBlobAsyncClient().setMetadataWithResponse(
                        Map.of("copyProgress", String.format("%.2f", percentageReady)), copyTaskParameters.getDestinationRequestConditions()))
                .doOnSuccess(resp -> responseCodeHandlerService.handleResponse(resp.getStatusCode()))
                .retryBackoff(imageCopyParameters.getRetryCount(), imageCopyParameters.getBackoffMinimum(), imageCopyParameters.getBackoffMaximum());
    }

    private Mono<Response<PageBlobItem>> sendToServer(CopyTaskParameters copyTaskParameters, PageRange r) {
        return copyTaskParameters.getPageBlobAsyncClient().uploadPagesFromUrlWithResponse(
                r,
                copyTaskParameters.getSourceBlobUrl(),
                r.getStart(),
                null,
                copyTaskParameters.getDestinationRequestConditions(),
                null
        );
    }
}

