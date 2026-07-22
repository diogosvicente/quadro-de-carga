package br.uerj.eletrica.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS liberado para qualquer origem.
 *
 * <p>Em produção o nginx serve front e API na mesma origem, mas requisições mutantes
 * (POST/PUT/DELETE) carregam o header {@code Origin} mesmo same-origin; atrás do proxy
 * (nginx/Cloudflare) o Spring vê troca de esquema/porta e as trata como cross-origin.
 * Com uma allow-list restrita isso vira <b>403</b> ao cadastrar/editar/excluir quando o
 * app é acessado por um host diferente do dev server (ex.: túnel de host dinâmico).
 *
 * <p>Como a API <b>não tem autenticação nem cookies</b> (nada a proteger por CORS) e é
 * compartilhada abertamente, liberar todas as origens elimina o 403 sem abrir superfície
 * nova: quem tem a URL já pode chamar a API diretamente. {@code allowedOriginPatterns("*")}
 * é a forma correta (o {@code allowedOrigins("*")} conflita com credenciais).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*");
    }
}
