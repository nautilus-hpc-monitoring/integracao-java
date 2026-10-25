package school.sptech.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import school.sptech.dto.jira.IssueFieldsDto;
import school.sptech.dto.jira.IssueRequestDto;
import school.sptech.dto.jira.IssueTypeDto;
import school.sptech.dto.jira.PriorityDto;
import school.sptech.dto.jira.ProjectDto;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public class JiraClient {

    private final String baseUrl;
    private final String authHeader;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public JiraClient(String baseUrl, String email, String apiToken) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String credenciais = email + ":" + apiToken;
        this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString(credenciais.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Cria a issue e devolve a key (ex.: LOG-12).
     * Usa a API v2 porque nela a descrição é texto simples.
     */
    public String createIssue(String projectKey, String summary, String description,
                              String issueTypeId, String prioridade) {
        try {
            IssueRequestDto body = new IssueRequestDto(new IssueFieldsDto(
                    new ProjectDto(projectKey),
                    summary,
                    description,
                    new IssueTypeDto(issueTypeId),
                    new PriorityDto(prioridade)));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/rest/api/2/issue"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return mapper.readTree(response.body()).get("key").asText();
            }
            throw new RuntimeException("Jira respondeu " + response.statusCode() + ": " + response.body());

        } catch (IOException e) {
            throw new RuntimeException("Falha ao chamar o Jira", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Chamada ao Jira interrompida", e);
        }
    }
}
