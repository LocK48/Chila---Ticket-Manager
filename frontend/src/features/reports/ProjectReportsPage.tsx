import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError, getSprintReport, listProjectSprints, type Sprint, type SprintReport } from "../../auth/api";

export function ProjectReportsPage() {
  const { workspaceId = "", projectId = "" } = useParams();
  const [sprints, setSprints] = useState<Sprint[]>([]);
  const [sprintId, setSprintId] = useState("");
  const [report, setReport] = useState<SprintReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const load = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const items = await listProjectSprints(projectId);
      setSprints(items);
      const selected = items.find((item) => item.id === sprintId) ?? items.find((item) => item.state === "ACTIVE") ?? items.find((item) => item.state === "CLOSED") ?? items[0];
      setSprintId(selected?.id ?? "");
      setReport(selected ? await getSprintReport(projectId, selected.id) : null);
    } catch (cause) { setError(cause instanceof ApiError ? cause.message : "Could not load sprint reports."); }
    finally { setLoading(false); }
  }, [projectId, sprintId]);
  useEffect(() => { void Promise.resolve().then(load); }, [load]);
  async function selectSprint(id: string) {
    setSprintId(id); setReport(null); setLoading(true); setError("");
    try { setReport(await getSprintReport(projectId, id)); }
    catch (cause) { setError(cause instanceof ApiError ? cause.message : "Could not load this sprint report."); }
    finally { setLoading(false); }
  }
  const maxRemaining = Math.max(1, ...(report?.burndown.map((point) => point.scopeIssues) ?? []));
  return <div className="report-page">
    <div className="report-heading"><div><p className="eyebrow">SCRUM REPORTS</p><h2>Sprint reports</h2><p className="subtle">Review the sprint’s issue-count burndown and completion snapshot.</p></div><Link className="secondary-button compact" to={`/workspaces/${workspaceId}/projects/${projectId}/backlog`}>Backlog</Link></div>
    <section className="report-card report-filter"><label htmlFor="sprint-report-select">Sprint</label><select id="sprint-report-select" value={sprintId} onChange={(event) => void selectSprint(event.target.value)} disabled={!sprints.length}><option value="">Select a sprint</option>{sprints.map((sprint) => <option key={sprint.id} value={sprint.id}>{sprint.name} · {sprint.state.toLowerCase()}</option>)}</select><button className="secondary-button compact" onClick={() => void load()} disabled={loading}>Refresh</button></section>
    {loading ? <p className="subtle">Loading report…</p> : error ? <div className="report-error" role="alert">{error}<button className="text-action" onClick={() => void load()}>Try again</button></div> : !report ? <div className="report-empty">Create a sprint to start collecting report history.</div> : <>
      {!report.historyAvailable && <div className="report-notice">This sprint has no captured reporting history. Reports are recorded from sprint activity going forward.</div>}
      {report.historyAvailable && <>
        <div className="report-stat-grid report-stat-grid-small"><ReportStat label="Issues in final scope" value={report.committedIssues ?? "—"} /><ReportStat label="Completed" value={report.completedIssues ?? "—"} /><ReportStat label="Unfinished" value={report.unfinishedIssues ?? "—"} /><ReportStat label="Completion" value={report.completionPercentage == null ? "—" : `${report.completionPercentage.toFixed(0)}%`} /></div>
        <section className="report-card"><div className="report-section-title"><div><h3>Burndown</h3><p className="subtle">Daily remaining issue count, including scope changes. This is not story points.</p></div></div>{report.burndown.length ? <div className="burndown-chart" role="img" aria-label={`Sprint burndown: ${report.burndown.at(-1)?.remainingIssues ?? 0} issues remaining on the last reported day`}>{report.burndown.map((point) => <div className="burndown-column" key={point.date} title={`${point.date}: ${point.remainingIssues} remaining of ${point.scopeIssues}`}><span>{point.remainingIssues}</span><i style={{ height: `${Math.max(3, point.remainingIssues / maxRemaining * 100)}%` }} /><small>{new Date(`${point.date}T00:00:00`).toLocaleDateString(undefined, { month: "short", day: "numeric" })}</small></div>)}</div> : <div className="report-empty">No daily points are available yet.</div>}</section>
        <section className="report-card"><div className="report-section-title"><div><h3>Completion snapshot</h3><p className="subtle">Issue statuses when the sprint was completed.</p></div></div>{report.completionIssues.length ? <div className="report-table-wrap"><table className="report-table"><thead><tr><th>Issue</th><th>Title</th><th>Status</th></tr></thead><tbody>{report.completionIssues.map((issue) => <tr key={issue.issueId}><td><Link to={`/workspaces/${workspaceId}/issues/${issue.issueKey}`}>{issue.issueKey}</Link></td><td>{issue.issueTitle}</td><td>{issue.issueStatus.replaceAll("_", " ").toLowerCase()}</td></tr>)}</tbody></table></div> : <p className="report-empty">No issue completion snapshot is available yet.</p>}</section>
      </>}
    </>}
  </div>;
}

function ReportStat({ label, value }: { label: string; value: string | number }) {
  return <article className="report-stat"><span>{label}</span><strong>{value}</strong></article>;
}
