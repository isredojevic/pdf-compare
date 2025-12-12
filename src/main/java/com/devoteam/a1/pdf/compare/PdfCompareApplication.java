package com.devoteam.a1.pdf.compare;

import com.devoteam.a1.pdf.compare.config.CompareProperties;
import com.devoteam.a1.pdf.compare.service.PdfCompareService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CompareProperties.class)
public class PdfCompareApplication implements CommandLineRunner {

	private final PdfCompareService service;

	public PdfCompareApplication(PdfCompareService service) {
		this.service = service;
	}

	public static void main(String[] args) {
		SpringApplication.run(PdfCompareApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		service.run();
	}
}
