package com.fortytwotalents.fotogallery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FotoGalleryQrCodeGeneratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(FotoGalleryQrCodeGeneratorApplication.class, args);
	}

}
