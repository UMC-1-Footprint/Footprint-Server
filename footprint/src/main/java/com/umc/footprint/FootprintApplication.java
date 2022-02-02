package com.umc.footprint;

import com.umc.footprint.config.properties.AppProperties;
import com.umc.footprint.config.properties.CorsProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
		CorsProperties.class,
		AppProperties.class
})
public class FootprintApplication {

	// 설정 파일 사용 (application.yml, aws.yml)
	public static final String APPLICATION_LOCATIONS = "spring.config.location="
			+ "classpath:application.yml,"
			+ "classpath:aws.yml";

	public static void main(String[] args) {
		new SpringApplicationBuilder(FootprintApplication.class)
				.properties(APPLICATION_LOCATIONS)
				.run(args);
	}

}
