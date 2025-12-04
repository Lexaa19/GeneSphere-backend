package com.gene.sphere.geneservice.cache;

import com.gene.sphere.geneservice.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SecurityConfigTest.TestEndpoints.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired MockMvc mvc;

    // Minimal endpoints to exercise your security rules
    @RestController
    static class TestEndpoints {
        @GetMapping("/actuator/health") public ResponseEntity<String> health() { return ResponseEntity.ok("ok"); }
        @GetMapping("/actuator/info")   public ResponseEntity<String> info()   { return ResponseEntity.ok("ok"); }
        @GetMapping("/genes/ping")      public ResponseEntity<String> genes()  { return ResponseEntity.ok("ok"); }
        @GetMapping("/cache/status")    public ResponseEntity<String> status() { return ResponseEntity.ok("ok"); }
        @DeleteMapping("/cache/delete/x") public ResponseEntity<Void> del()    { return ResponseEntity.noContent().build(); }
        @DeleteMapping("/cache/clear/x")  public ResponseEntity<Void> clear()  { return ResponseEntity.noContent().build(); }
    }

    // Test-only users; overrides any prod user store if present
    @TestConfiguration
    static class TestUsers {
        @Bean @Primary
        UserDetailsService uds(PasswordEncoder encoder) {
            return new InMemoryUserDetailsManager(
                    User.withUsername("admin").password(encoder.encode("adminpass")).roles("ADMIN").build(),
                    User.withUsername("user").password(encoder.encode("userpass")).roles("USER").build()
            );
        }
    }

    @Test
    void actuator_requires_admin_in_your_config() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());

        mvc.perform(get("/actuator/health").with(httpBasic("user","userpass"))).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/health").with(httpBasic("admin","adminpass"))).andExpect(status().isOk());
        mvc.perform(get("/actuator/info").with(httpBasic("admin","adminpass"))).andExpect(status().isOk());
    }

    @Test
    void user_endpoints() throws Exception {
        mvc.perform(get("/genes/ping")).andExpect(status().isUnauthorized());
        mvc.perform(get("/cache/status")).andExpect(status().isUnauthorized());

        mvc.perform(get("/genes/ping").with(httpBasic("user","userpass"))).andExpect(status().isOk());
        mvc.perform(get("/cache/status").with(httpBasic("user","userpass"))).andExpect(status().isOk());
    }

    @Test
    void admin_endpoints() throws Exception {
        mvc.perform(delete("/cache/delete/x").with(httpBasic("user","userpass"))).andExpect(status().isForbidden());
        mvc.perform(delete("/cache/clear/x").with(httpBasic("user","userpass"))).andExpect(status().isForbidden());

        mvc.perform(delete("/cache/delete/x").with(httpBasic("admin","adminpass"))).andExpect(status().isNoContent());
        mvc.perform(delete("/cache/clear/x").with(httpBasic("admin","adminpass"))).andExpect(status().isNoContent());
    }
}