package com.larco.biblioteca.external;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/** Cliente HTTP del servicio externo (se ejecuta localmente en /external/legacy-bank). */
@Component
public class LegacyBankHttpClient implements LegacyBankApi {

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final ObjectMapper mapper;
    private final String baseUrl;

    public LegacyBankHttpClient(ObjectMapper mapper, @Value("${app.external.base-url}") String baseUrl) {
        this.mapper = mapper;
        this.baseUrl = baseUrl;
    }

    @Override
    public Map<String, Object> executeTransaction(long valueInCents, String code) {
        try {
            String url = baseUrl + "/executeTransaction?value=" + valueInCents + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(3))
                    .POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 500) {
                throw new ExternalServiceException("Servicio externo respondió HTTP " + res.statusCode());
            }
            return mapper.readValue(res.body(), new TypeReference<Map<String, Object>>() { });
        } catch (ExternalServiceException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("Llamada interrumpida");
        } catch (Exception e) {
            throw new ExternalServiceException("Servicio externo no disponible: " + e.getMessage());
        }
    }
}
