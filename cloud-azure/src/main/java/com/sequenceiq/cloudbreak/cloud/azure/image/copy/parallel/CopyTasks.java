package com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.models.PageBlobItem;
import com.azure.storage.blob.models.PageBlobRequestConditions;
import com.azure.storage.blob.models.PageRange;
import com.azure.storage.blob.specialized.BlobLeaseAsyncClient;
import com.azure.storage.blob.specialized.PageBlobAsyncClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CopyTasks {

    private final PageBlobAsyncClient destinationClient;

    private final String sourceBlobUrl;

    private final PageBlobRequestConditions destinationRequestConditions;

    public CopyTasks(PageBlobAsyncClient destinationClient, String sourceBlobUrl, PageBlobRequestConditions destinationRequestConditions) {
        this.destinationClient = destinationClient;
        this.sourceBlobUrl = sourceBlobUrl;
        this.destinationRequestConditions = destinationRequestConditions;
    }

    public Mono<Void> createAll(List<PageRange> copyRanges, BlobLeaseAsyncClient blobLeaseAsyncClient) {

        AtomicLong successCounter = new AtomicLong();
        AtomicBoolean imageCopyRunning = new AtomicBoolean(true);
        return Flux.just(getLeaseRenewFlux(blobLeaseAsyncClient, imageCopyRunning), getCopyFlux(copyRanges, successCounter, imageCopyRunning))
            .flatMap(x -> x, 2, 2)
            .then();
    }

    private Mono<Void> getLeaseRenewFlux(BlobLeaseAsyncClient blobLeaseAsyncClient, AtomicBoolean copyRunning) {
        return Mono.just(blobLeaseAsyncClient)
            .delayElement(Duration.of(5, ChronoUnit.SECONDS))
            .flatMap(client -> client.renewLeaseWithResponse(null))
            .doOnSuccess(resp -> checkResponse(resp.getStatusCode()))
            .retryBackoff(10, Duration.of(1, ChronoUnit.SECONDS), Duration.of(10, ChronoUnit.SECONDS))
            .repeat(copyRunning::get)
            .then(Mono.just(blobLeaseAsyncClient))
            .flatMap(x -> blobLeaseAsyncClient.releaseLeaseWithResponse(destinationRequestConditions))
            .doOnSuccess(resp -> checkResponse(resp.getStatusCode()))
            .retryBackoff(10, Duration.of(1, ChronoUnit.SECONDS), Duration.of(10, ChronoUnit.SECONDS))
            .then();
    }

    private Mono<Void> getCopyFlux(List<PageRange> copyRanges, AtomicLong successCounter, AtomicBoolean imageCopyRunning) {
        return Flux.fromIterable(copyRanges)
                .flatMap(r -> {
                        Mono<Void> response = Mono.just(r)
                            .flatMap(this::sendToServer)
                            .doOnSuccess(resp -> checkResponse(resp.getStatusCode()))
                            .retryBackoff(10, Duration.of(1, ChronoUnit.SECONDS), Duration.of(10, ChronoUnit.SECONDS))
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
                    , 200, 400)
                .doOnComplete(() -> System.out.println("-+- onComplete"))
            .doFinally(s -> imageCopyRunning.set(false))
            .then()
//                .doOnEach(s -> System.out.println("-+- doOnEach:" + s))
//                .doOnNext(s -> {
//                    System.out.println("-+- onNext: " + s);
//                    try {
//                        System.out.println("Sleeping a long one");
//                        Thread.sleep(10000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                })
                ;
    }

    private void checkResponse(int responseCode) {
        if (responseCode == 503) {
            throw new RuntimeException("Return code is 503");
        }
    }

    private Mono<Response<Void>> sendProgressInfo(Double percentageReady) {
        return Mono.just(percentageReady)
            .flatMap(p -> destinationClient.setMetadataWithResponse(Map.of("copyProgress", String.format("%.2f", percentageReady)), destinationRequestConditions))
            .doOnSuccess(resp -> checkResponse(resp.getStatusCode()))
            .retryBackoff(10, Duration.of(1, ChronoUnit.SECONDS), Duration.of(10, ChronoUnit.SECONDS));

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

    private Mono<Response<PageBlobItem>> sendToServerFake(PageRange r) {
        System.out.println("*** Executing the call now");
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        return Mono.just(getResponse(503));
        return Mono.just(getResponse(201));
    }

    private Response<PageBlobItem> getResponse(int status) {
        return new Response<PageBlobItem>() {
            @Override
            public int getStatusCode() {
                return status;
            }

            @Override
            public HttpHeaders getHeaders() {
                return null;
            }

            @Override
            public HttpRequest getRequest() {
                return null;
            }

            @Override
            public PageBlobItem getValue() {
                return null;
            }
        };
    }
}

