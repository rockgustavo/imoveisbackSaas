package br.com.rockgustavo.imobiliaria.shared.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcSecurityConfig implements WebMvcConfigurer {

    private final AcessoAtivoInterceptor acessoAtivoInterceptor;

    public WebMvcSecurityConfig(AcessoAtivoInterceptor acessoAtivoInterceptor) {
        this.acessoAtivoInterceptor = acessoAtivoInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(acessoAtivoInterceptor);
    }
}
