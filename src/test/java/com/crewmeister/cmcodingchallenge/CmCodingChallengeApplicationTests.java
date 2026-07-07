package com.crewmeister.cmcodingchallenge;

import com.crewmeister.cmcodingchallenge.currency.client.BundesbankApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
@AutoConfigureTestDatabase
class CmCodingChallengeApplicationTests {

	@MockBean
	BundesbankApiClient apiClient;

	@Test
	void contextLoads() {
	}

}
