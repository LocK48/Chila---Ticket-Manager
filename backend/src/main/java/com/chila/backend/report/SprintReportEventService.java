package com.chila.backend.report;

import com.chila.backend.issue.IssueEntity;
import com.chila.backend.sprint.SprintEntity;
import com.chila.backend.sprint.SprintState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SprintReportEventService {
    private final SprintReportEventRepository repository;
    public SprintReportEventService(SprintReportEventRepository repository) { this.repository = repository; }

    @Transactional
    public void record(SprintEntity sprint, IssueEntity issue, SprintReportEventKind kind, boolean inScope) {
        if (sprint != null) repository.save(new SprintReportEventEntity(sprint, issue, kind, inScope));
    }

    @Transactional
    public void statusChanged(IssueEntity issue) {
        if (issue.getSprint() != null && issue.getSprint().getState() == SprintState.ACTIVE)
            record(issue.getSprint(), issue, SprintReportEventKind.STATUS_CHANGED, true);
    }
}
