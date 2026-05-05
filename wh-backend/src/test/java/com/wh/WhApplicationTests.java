package com.wh;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"sqlite", "dev"})
class WhApplicationTests {

    @Test
    void contextLoads() {
    }
}
