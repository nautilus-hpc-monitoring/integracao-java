package school.sptech.dto.jira;

public record IssueFieldsDto(ProjectDto project, String summary, String description,
                             IssueTypeDto issuetype, PriorityDto priority) {}
