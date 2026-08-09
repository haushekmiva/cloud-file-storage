package com.haushekmiva.cloudfilestorage;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	public static final String MINIO_ROOT_USER = "testuser";
	public static final String MINIO_ROOT_PASSWORD = "testpassword";

	public static GenericContainer<?> minioContainer =
			new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
					.withExposedPorts(9000)
					.withEnv("MINIO_ROOT_USER", MINIO_ROOT_USER)
					.withEnv("MINIO_ROOT_PASSWORD", MINIO_ROOT_PASSWORD)
					.withCommand("server", "/data")
					.waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

	static {
		minioContainer.start();
	}

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
	}

}
