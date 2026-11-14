package dojo;

import java.util.List;

public record IssuePage(List<IssueView> items, int page, int size, long total) {}
