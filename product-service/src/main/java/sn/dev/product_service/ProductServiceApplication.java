package sn.dev.product_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import sn.dev.product_service.config.RSAKeysConfig;
import sn.dev.product_service.data.entities.Product;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableConfigurationProperties(RSAKeysConfig.class)
public class ProductServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductServiceApplication.class, args);
	}

	@Bean
	CommandLineRunner initData(MongoTemplate mongoTemplate) {
		return args -> {
			// Migration: Set version to 0 for documents that don't have it
			Query query = new Query();
			query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("version").exists(false));
			Update update = new Update().set("version", 0L);
			mongoTemplate.updateMulti(query, update, Product.class);
		};
	}

}
