package com.gpintus.sqscustomendpoint;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = {SqsCustomEndpointApplicationTests.SpringContextInitializer.class})
@Testcontainers
@Slf4j
class SqsCustomEndpointApplicationTests {
	private static final DockerImageName localstackImage = DockerImageName.parse("localstack/localstack");

	@org.testcontainers.junit.jupiter.Container
	public static LocalStackContainer localstackContainer = new LocalStackContainer(localstackImage)
			.withServices(LocalStackContainer.Service.SQS);

	static class SpringContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		@Override
		@SneakyThrows
		public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
			ConfigurableEnvironment environment = configurableApplicationContext.getEnvironment();

			// in real life, typically the queues are there before to start the application
			execInContainer(localstackContainer,"awslocal", "sqs", "create-queue", "--queue-name", environment.getProperty("app.queueName"));

			TestPropertyValues.of(
					"cloud.aws.credentials.access-key=" + localstackContainer.getAccessKey(),
					"cloud.aws.credentials.secret-key=" + localstackContainer.getSecretKey(),
					"cloud.aws.sqs.region=" + localstackContainer.getRegion(),
					"cloud.aws.sqs.endpoint=" + localstackContainer.getEndpointConfiguration(LocalStackContainer.Service.SQS).getServiceEndpoint()
			).applyTo(environment);
		}
	}

	@SneakyThrows
	private static Container.ExecResult execInContainer(ContainerState container, String... parameters) {
		Container.ExecResult execResult = container.execInContainer(parameters);
		Assertions.assertThat(execResult.getExitCode()).isZero();
		return execResult;
	}

	@Autowired
	private AmazonSQS amazonSQS;
	@Autowired
	private AppProperties appProperties;

	@Test
	@SneakyThrows
	void contextLoads() {
		{
			ListQueuesResult res = amazonSQS.listQueues();
			Assertions.assertThat(res.getQueueUrls()).hasSize(1);
			Assertions.assertThat(res.getQueueUrls().get(0)).contains(appProperties.getQueueName());
		}

		{
			CreateQueueResult createQueueResult = amazonSQS.createQueue("anotherQueue");
			Assertions.assertThat(createQueueResult.getQueueUrl()).isNotEmpty();
		}

		{
			ListQueuesResult res = amazonSQS.listQueues();
			Assertions.assertThat(res.getQueueUrls()).hasSize(2);
		}

		{
			Container.ExecResult execResult = execInContainer(localstackContainer, "awslocal", "sqs", "list-queues");
			Assertions.assertThat(execResult.getStdout()).contains("anotherQueue").contains(appProperties.getQueueName());
		}

		// debug purposes
		String region = localstackContainer.getRegion(); // the same set in cloud.aws.sqs.region property

		AmazonSQSAsyncClient realSQS = (AmazonSQSAsyncClient) ReflectionTestUtils.getField(amazonSQS, "realSQS");
		String signerRegionOverride = realSQS.getSignerRegionOverride(); // "null" is the expected value?
		String signingRegion = (String) ReflectionTestUtils.getField(realSQS, "signingRegion"); // "null" is the expected value?
	}

}
