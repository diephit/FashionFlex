package g6.fashionFlex.config;

import g6.fashionFlex.security.CustomOAuth2UserService;
import g6.fashionFlex.security.CustomUserDetailsService;
import g6.fashionFlex.security.JwtAuthenticationEntryPoint;
import g6.fashionFlex.security.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    // @Autowired
    // private JwtAuthenticationEntryPoint unauthorizedHandler;

    // @Bean
    // public JwtAuthenticationFilter jwtAuthenticationFilter() {
    //     return new JwtAuthenticationFilter();
    // }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CustomOAuth2UserService customOAuth2UserService() {
        return new CustomOAuth2UserService();
    }

    @Bean
    public OAuth2LoginSuccessHandler oauth2LoginSuccessHandler() {
        return new OAuth2LoginSuccessHandler();
    }

    @Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                    // Public pages - ĐẶT TRƯỚC để ưu tiên
                    .requestMatchers("/", "/index", "/home", "/home-02", "/home-03").permitAll()
                    .requestMatchers("/about", "/contact", "/blog", "/blog-detail").permitAll()
                    .requestMatchers("/product", "/products", "/product/**").permitAll()

                    // Auth & Password Reset - QUAN TRỌNG: đặt trước anyRequest()
                    .requestMatchers("/login", "/register", "/api/auth/**").permitAll()
                    .requestMatchers("/forgot-password", "/verify-token", "/reset-password", "/resend-code", "/check-cooldown").permitAll()

                    // Static resources
                    .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/vendor/**", "/assets/**").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()

                    // User pages - require authentication
                    .requestMatchers("/user/**").authenticated()
                    .requestMatchers("/wishlist/**").authenticated()

                    // Cart: allow viewing page, but protect API operations
                    .requestMatchers("/cart", "/shopping-cart").permitAll()  // Allow viewing cart page
                    .requestMatchers("/api/cart/**").authenticated()  // Protect cart API operations
                    .requestMatchers("/cart/**").authenticated()  // Protect other cart actions

                    .requestMatchers("/checkout/**").authenticated()
                    .requestMatchers("/order/**").authenticated()

                    // Admin pages - both web UI and API
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")

                    // Tất cả các request khác cần authentication
                    .anyRequest().authenticated()
            )
            .formLogin(form -> form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .usernameParameter("email")
                    .passwordParameter("password")
                    .defaultSuccessUrl("/", false)  // false = redirect to original requested page
                    .failureUrl("/login?error=true")
                    .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .defaultSuccessUrl("/", true)
                    .failureUrl("/login?error=true")
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService())
                    )
                    .successHandler(oauth2LoginSuccessHandler())
            )
            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout=true")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
            );

    http.authenticationProvider(authenticationProvider());
    http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

    return http.build();
}

}
