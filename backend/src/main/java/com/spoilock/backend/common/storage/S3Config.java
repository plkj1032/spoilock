package com.spoilock.backend.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Bean(destroyMethod = "close")
    public S3Client s3Client(
            @Value("${app.media.s3.region}") String region
    )
    {
        return S3Client.builder()
                .region(Region.of(region))
                // EC2에서는 인스턴스 IAM 역할 사용
                .build();
    }
    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(
            @Value("${app.media.s3.region}") String region
    ) {
        return S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }
}
