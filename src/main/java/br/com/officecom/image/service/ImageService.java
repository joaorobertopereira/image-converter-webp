package br.com.officecom.image.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);
    private final S3AsyncClient s3AsyncClient;
    private final AtomicInteger processedCount = new AtomicInteger(0);

    @Value("${wasabi.bucket-name}")
    private String bucketName;

    public ImageService(S3AsyncClient s3AsyncClient) {
        this.s3AsyncClient = s3AsyncClient;
    }

    public Mono<Void> processAllImages() {
        processedCount.set(0);
        return listAllObjectsFlux()
                .filter(key -> {
                    String lowerKey = key.toLowerCase();
                    return lowerKey.endsWith(".png") || lowerKey.endsWith(".jpg") || lowerKey.endsWith(".jpeg") || lowerKey.endsWith(".pdf");
                })
                .flatMap(key -> processImage(key)
                        .doOnSuccess(v -> {
                            int count = processedCount.incrementAndGet();
                            log.info("Processed image {}: {}", count, key);
                        })
                        .onErrorResume(e -> {
                            log.error("Error processing image {}: {}", key, e.getMessage());
                            return Mono.empty();
                        }), 100)
                .then();
    }

    public Mono<Void> processImage(String key) {
        return downloadImage(key)
                .flatMap(bytes -> 
                    Mono.fromCallable(() -> convertToWebp(key, bytes))
                        .subscribeOn(Schedulers.boundedElastic())
                )
                .flatMap(webpBytes -> {
                    String keyWithoutExtension = key.substring(0, key.lastIndexOf('.'));
                    String webpKey = "webp/" + keyWithoutExtension + ".webp";
                    
                    return uploadImage(webpKey, webpBytes);
                });
    }

    public Mono<List<String>> listObjects() {
        return listAllObjectsFlux().collectList();
    }

    private Flux<String> listAllObjectsFlux() {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();

        return Flux.from(s3AsyncClient.listObjectsV2Paginator(listObjectsV2Request))
                .flatMap(response -> Flux.fromIterable(response.contents()))
                .map(S3Object::key);
    }

    private Mono<byte[]> downloadImage(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        CompletableFuture<byte[]> future = s3AsyncClient.getObject(getObjectRequest, AsyncResponseTransformer.toBytes())
                .thenApply(response -> response.asByteArray());

        return Mono.fromFuture(future);
    }

    private Mono<Void> uploadImage(String key, byte[] bytes) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("image/webp")
                .build();

        CompletableFuture<Void> future = s3AsyncClient.putObject(putObjectRequest, AsyncRequestBody.fromBytes(bytes))
                .thenAccept(response -> {});

        return Mono.fromFuture(future);
    }

    private byte[] convertToWebp(String key, byte[] inputBytes) throws IOException {
        String lowerKey = key.toLowerCase();
        if (lowerKey.endsWith(".pdf")) {
            return convertPdfToWebp(inputBytes);
        } else {
            return convertImageToWebp(inputBytes);
        }
    }

    private byte[] convertImageToWebp(byte[] imageBytes) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
        BufferedImage image = ImageIO.read(inputStream);

        if (image == null) {
            throw new IOException("Could not read image. Ensure the format is supported (e.g., JPG, PNG).");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "webp", outputStream)) {
             throw new IOException("Could not write webp image. Ensure webp-imageio dependency is present.");
        }
        return outputStream.toByteArray();
    }

    private byte[] convertPdfToWebp(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            // Convert only the first page for now
            BufferedImage image = pdfRenderer.renderImageWithDPI(0, 300, ImageType.RGB);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "webp", outputStream)) {
                throw new IOException("Could not write webp image from PDF.");
            }
            return outputStream.toByteArray();
        }
    }
}
