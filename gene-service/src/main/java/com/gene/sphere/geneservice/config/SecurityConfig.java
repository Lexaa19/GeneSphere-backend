package com.gene.sphere.geneservice.config;

import com.gene.sphere.geneservice.security.JwtAuthenticationEntryPoint;
import com.gene.sphere.geneservice.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Value("${app.security.admin.username}")
    private String adminUsername;

    @Value("${app.security.admin.password}")
    private String adminPassword;

    @Value("${app.security.user.username}")
    private String userUsername;

    @Value("${app.security.user.password}")
    private String userPassword;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        PasswordEncoder encoder = passwordEncoder();

        System.out.println("=== User Details Service Created ===");
        System.out.println("Admin username: " + adminUsername);
        System.out.println("Admin password (plain): " + adminPassword);
        System.out.println("User username: " + userUsername);
        System.out.println("User password (plain): " + userPassword);

        UserDetails admin = User.builder()
                .username(adminUsername)
                .password(encoder.encode(adminPassword))
                .roles("ADMIN", "USER")
                .build();

        UserDetails user = User.builder()
                .username(userUsername)
                .password(encoder.encode(userPassword))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(admin, user);
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder
                .userDetailsService(userDetailsService())
                .passwordEncoder(passwordEncoder());
        return authBuilder.build();
    }
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {

    http
            .csrf().disable()
            .authorizeRequests()
                    .antMatchers("/auth/**").permitAll()
                    .antMatchers("/api/genes/**").permitAll() 
                    .antMatchers("/actuator/health", "/actuator/info").permitAll()
                    .antMatchers("/admin/**", "/actuator/**").hasRole("ADMIN")
                    .antMatchers("/api/**").hasAnyRole("USER", "ADMIN")
                    .anyRequest().authenticated()
            .and()
            .exceptionHandling()
                    .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers().frameOptions().deny();

    return http.build();
}
}