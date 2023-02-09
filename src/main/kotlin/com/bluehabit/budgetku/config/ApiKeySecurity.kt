package com.bluehabit.budgetku.config

import com.bluehabit.budgetku.admin.apiKey.v1.ApiKeyRepository
import com.bluehabit.budgetku.config.apiKeyMiddleware.ApiKeyAuthFilter
import com.bluehabit.budgetku.config.apiKeyMiddleware.ApiKeyAuthManager
import com.bluehabit.budgetku.config.apiKeyMiddleware.ApiKeyFilterChainExceptionHandler
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.authentication.logout.LogoutFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@Order(1)
class ApiKeySecurity(
    private val apiKeyRepository: ApiKeyRepository,
    private val filterException: ApiKeyFilterChainExceptionHandler
):WebSecurityConfigurerAdapter(){
    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {

        val apiKeyAuthFilter = ApiKeyAuthFilter("x-api-key")
        apiKeyAuthFilter.setAuthenticationManager(ApiKeyAuthManager(apiKeyRepository))

        val corsOrigin = "*"

        http
            .addFilterBefore(filterException,LogoutFilter::class.java)
            .csrf()
            .disable()
            .authorizeRequests()
                .and()
                    .antMatcher("/api/v1/**")
                    .addFilter(apiKeyAuthFilter)
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                    .cors()
                    .configurationSource(corsConfigurationSource(corsOrigin))
                .and()
                    .authorizeRequests()
                    .anyRequest()
                    .authenticated()


    }
    private fun corsConfigurationSource(corsOrigin: String): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.addAllowedOrigin("*")
        configuration.allowedOrigins = listOf(corsOrigin)
        configuration.allowedMethods = listOf(
            "GET", "POST", "HEAD", "OPTIONS", "PUT", "PATCH", "DELETE"
        )
        configuration.maxAge = 10L
        //when this true the origin = * cannot be used any more
        // configuration.allowCredentials = true
        configuration.allowedHeaders = listOf(
            "Accept",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "Access-Control-Allow-Origin",
            "Access-Control-Expose-Headers",
            "Accept-Language",
            "Authorization",
            "Content-Type",
            "Request-Name",
            "Request-Surname",
            "Origin",
            "X-Api-Key",
            "X-Request-AppVersion",
            "X-Request-OsVersion",
            "X-Request-Device",
            "X-Requested-With"
        )
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)

        return source
    }
}