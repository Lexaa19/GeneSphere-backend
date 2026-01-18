package com.gene.sphere.geneservice.config;

import com.gene.sphere.geneservice.cache.RedisCacheService;
import com.gene.sphere.geneservice.security.JwtAuthenticationFilter;
import com.gene.sphere.geneservice.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = SecurityConfigTest.TestApp.class,
        properties = {
                "app.security.admin.username=admin",
                "app.security.admin.password=admin123",
                "app.security.user.username=user",
                "app.security.user.password=user123"
        })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @MockBean
    private RedisCacheService redisCacheService;
    
    @MockBean
    private RedisHealthIndicator redisHealthIndicator;

    @Test
    void adminCanAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/admin/config")
                        .with(httpBasic("admin", "admin123"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/admin/config")
                        .with(httpBasic("user", "user123"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanAccessGeneEndpoints() throws Exception {
        mockMvc.perform(get("/genes/BRCA1")
                        .with(httpBasic("user", "user123"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/genes/BRCA1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorHealthRequiresAdmin() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .with(httpBasic("user", "user123"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void userDetailsServiceEncodesPasswords() {
        var adminDetails = userDetailsService.loadUserByUsername("admin");
        var userDetails = userDetailsService.loadUserByUsername("user");

        assertTrue(passwordEncoder.matches("admin123", adminDetails.getPassword()));
        assertTrue(passwordEncoder.matches("user123", userDetails.getPassword()));
    }

    @Configuration
    @EnableAutoConfiguration
    @Import(SecurityConfig.class)
    static class TestApp {

        @RestController
        static class StubController {

            @GetMapping("/genes/{id}")
            String gene(@PathVariable String id) {
                return "gene-" + id;
            }

            @GetMapping("/admin/config")
            String adminConfig() {
                return "admin";
            }

            @GetMapping("/actuator/health")
            String health() {
                return "ok";
            }
        }
    }
}