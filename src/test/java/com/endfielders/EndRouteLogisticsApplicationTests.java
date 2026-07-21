package com.endfielders;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "gemini.api.key=TEST_KEY",
    "weather.api.key=TEST_KEY"
})
class EndRouteLogisticsApplicationTests {

    @Test
    void contextLoads() {
    }

}
