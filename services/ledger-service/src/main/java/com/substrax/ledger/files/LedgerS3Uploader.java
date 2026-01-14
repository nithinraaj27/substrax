package com.substrax.ledger.files;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerS3Uploader {

    private final S3Client client;

    @Value("${ledger.export.s3.bucket}")
    private String bucket;

    public void upload(Path filePath){

        String key = buildS3Key(filePath);

        PutObjectRequest request = PutObjectRequest.builder().bucket(bucket).key(key).contentType("appliction/json").build();

        client.putObject(request, filePath);

        log.info(
                "Ledger batch uploaded to S3 successfully. bucket={}, key={}",
                bucket,
                key
        );
    }

    private String buildS3Key(Path filePath){

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        return String.format("raw/ledger/year=%d/month=%02d/day=%02d/%s", today.getYear(), today.getMonthValue(), today.getDayOfMonth(), filePath.getFileName().toString());
    }
}
