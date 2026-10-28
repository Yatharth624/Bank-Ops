import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { apiClient } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { ExceptionCase, CaseTimelineEvent, User } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import { SeverityBadge } from '../components/SeverityBadge';
import {
  ArrowLeft,
  AlertTriangle,
  UserCheck,
  CheckCircle,
  Clock,
  Send,
  MessageSquare,
  AlertCircle,
  RotateCcw,
} from 'lucide-react';

export const CaseDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, isAdmin } = useAuth();

  const [caseData, setCaseData] = useState<ExceptionCase | null>(null);
  const [timeline, setTimeline] = useState<CaseTimelineEvent[]>([]);
  const [staffUsers, setStaffUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);

  // Form States
  const [newNote, setNewNote] = useState('');
  const [submittingNote, setSubmittingNote] = useState(false);
  const [resolutionSummary, setResolutionSummary] = useState('');
  const [reopenJustification, setReopenJustification] = useState('');
  const [selectedAnalystId, setSelectedAnalystId] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const fetchCaseDetails = async () => {
    setLoading(true);
    setErrorMsg(null);
    try {
      const [cRes, tRes] = await Promise.all([
        apiClient.get(`/exceptions/${id}`),
        apiClient.get(`/exceptions/${id}/timeline`),
      ]);

      if (cRes.data.success) {
        setCaseData(cRes.data.data);
        if (cRes.data.data.assignedAnalystId) {
          setSelectedAnalystId(cRes.data.data.assignedAnalystId);
        }
      }
      if (tRes.data.success) setTimeline(tRes.data.data);

      if (isAdmin) {
        const uRes = await apiClient.get('/admin/users');
        if (uRes.data.success) setStaffUsers(uRes.data.data);
      }
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || 'Failed to load case investigation details');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (id) fetchCaseDetails();
  }, [id]);

  const handleAddNote = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newNote.trim()) return;

    setSubmittingNote(true);
    setErrorMsg(null);
    try {
      const res = await apiClient.post(`/exceptions/${id}/notes`, { noteText: newNote });
      if (res.data.success) {
        setNewNote('');
        fetchCaseDetails();
      }
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || 'Failed to submit investigation note');
    } finally {
      setSubmittingNote(false);
    }
  };

  const handleAssignAnalyst = async () => {
    if (!selectedAnalystId || !caseData) return;
    setErrorMsg(null);
    try {
      const res = await apiClient.post(`/exceptions/${id}/assign`, {
        analystId: selectedAnalystId,
        expectedVersion: caseData.version,
      });
      if (res.data.success) {
        setSuccessMsg('Analyst successfully assigned');
        setTimeout(() => setSuccessMsg(null), 3000);
        fetchCaseDetails();
      }
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || 'Failed to assign analyst. Possible concurrent modification.');
    }
  };

  const handleStatusTransition = async (targetStatus: string) => {
    if (!caseData) return;
    setErrorMsg(null);

    const payload: any = {
      targetStatus,
      expectedVersion: caseData.version,
    };

    if (targetStatus === 'RESOLVED') {
      if (!resolutionSummary.trim()) {
        setErrorMsg('Resolution summary is mandatory to resolve a case.');
        return;
      }
      payload.resolutionSummary = resolutionSummary.trim();
    }

    if (targetStatus === 'REOPENED') {
      if (!reopenJustification.trim()) {
        setErrorMsg('Documented justification is mandatory when reopening a resolved case.');
        return;
      }
      payload.justification = reopenJustification.trim();
    }

    try {
      const res = await apiClient.patch(`/exceptions/${id}/status`, payload);
      if (res.data.success) {
        setSuccessMsg(`Status successfully transitioned to ${targetStatus}`);
        setResolutionSummary('');
        setReopenJustification('');
        setTimeout(() => setSuccessMsg(null), 3000);
        fetchCaseDetails();
      }
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || 'Failed to transition workflow status.');
    }
  };

  if (loading) {
    return <div className="text-center py-12 text-slate-400">Loading investigation case...</div>;
  }

  if (!caseData) {
    return (
      <div className="text-center py-12">
        <p className="text-rose-400 font-semibold mb-4">Case not found</p>
        <button
          onClick={() => navigate('/exceptions')}
          className="px-4 py-2 bg-slate-800 text-slate-200 rounded-lg text-xs"
        >
          Return to Queue
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      {/* Top Breadcrumb & Actions */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => navigate('/exceptions')}
          className="flex items-center gap-2 text-xs font-medium text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft size={16} />
          <span>Back to Exception Queue</span>
        </button>

        <div className="flex items-center gap-2 text-xs font-mono text-slate-400">
          <span>Concurrency Version:</span>
          <span className="text-sky-400 font-bold">v{caseData.version}</span>
        </div>
      </div>

      {errorMsg && (
        <div className="p-4 rounded-xl bg-rose-950/50 border border-rose-800 text-rose-300 text-xs flex items-center gap-3">
          <AlertCircle size={18} className="shrink-0 text-rose-400" />
          <span>{errorMsg}</span>
        </div>
      )}

      {successMsg && (
        <div className="p-4 rounded-xl bg-emerald-950/50 border border-emerald-800 text-emerald-300 text-xs flex items-center gap-3">
          <CheckCircle size={18} className="shrink-0 text-emerald-400" />
          <span>{successMsg}</span>
        </div>
      )}

      {/* Case Header Card */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-5 mb-5">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold font-mono text-white">{caseData.caseNumber}</h1>
              <StatusBadge status={caseData.status} />
              <SeverityBadge severity={caseData.severity} />
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Category: <strong className="text-slate-200">{caseData.category.replace(/_/g, ' ')}</strong> &bull; Rule: <strong className="text-sky-400 font-mono">{caseData.ruleCode || 'OPERATIONAL_CHECK'}</strong>
            </p>
          </div>

          <div className="text-right text-xs text-slate-400 font-mono">
            <div>Created: {new Date(caseData.createdAt).toLocaleString()}</div>
            <div>Updated: {new Date(caseData.updatedAt).toLocaleString()}</div>
          </div>
        </div>

        {/* Related Transaction Metadata */}
        {caseData.transaction && (
          <div className="bg-slate-950/60 rounded-xl p-4 border border-slate-800/80">
            <h3 className="text-xs font-semibold text-slate-300 uppercase tracking-wider mb-3">
              Associated Synthetic Transaction Telemetry
            </h3>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs font-mono">
              <div>
                <span className="text-slate-500 block text-[10px] uppercase font-sans">Reference:</span>
                <span className="text-sky-400 font-semibold">{caseData.transaction.externalReference}</span>
              </div>
              <div>
                <span className="text-slate-500 block text-[10px] uppercase font-sans">Amount:</span>
                <span className="text-white font-bold">
                  {caseData.transaction.currency} {Number(caseData.transaction.amount).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </span>
              </div>
              <div>
                <span className="text-slate-500 block text-[10px] uppercase font-sans">Channel & Type:</span>
                <span className="text-slate-300">{caseData.transaction.channel} / {caseData.transaction.transactionType}</span>
              </div>
              <div>
                <span className="text-slate-500 block text-[10px] uppercase font-sans">Tx Status:</span>
                <StatusBadge status={caseData.transaction.status} />
              </div>
            </div>

            {caseData.transaction.failureReason && (
              <div className="mt-3 text-xs bg-rose-950/30 border border-rose-900/60 p-2.5 rounded text-rose-300">
                <strong>Exception Reason:</strong> {caseData.transaction.failureReason}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Workflow Operations Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Assignment & Status Box */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-sm space-y-6">
          <h2 className="text-sm font-bold text-white uppercase tracking-wider flex items-center gap-2">
            <UserCheck size={18} className="text-sky-400" />
            <span>Workflow & Assignment Controls</span>
          </h2>

          {/* Current Assignee */}
          <div className="p-4 rounded-xl bg-slate-800/50 border border-slate-700/80">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block mb-1">
              Assigned Lead Analyst
            </span>
            <div className="text-sm font-semibold text-white">
              {caseData.assignedAnalystFullName ? (
                <span>{caseData.assignedAnalystFullName} (@{caseData.assignedAnalystUsername})</span>
              ) : (
                <span className="text-amber-400">Unassigned (In Triage Queue)</span>
              )}
            </div>

            {isAdmin && staffUsers.length > 0 && (
              <div className="mt-3 flex gap-2">
                <select
                  value={selectedAnalystId}
                  onChange={(e) => setSelectedAnalystId(e.target.value)}
                  className="flex-1 px-3 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-xs text-slate-200"
                >
                  <option value="">Select Analyst...</option>
                  {staffUsers.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.fullName} (@{u.username})
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  onClick={handleAssignAnalyst}
                  className="px-3 py-1.5 bg-sky-600 hover:bg-sky-500 text-white rounded-lg text-xs font-semibold transition-colors"
                >
                  Assign
                </button>
              </div>
            )}
          </div>

          {/* State Transition Actions */}
          <div className="space-y-4">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
              Workflow Status Transition
            </span>

            {/* OPEN -> IN_REVIEW */}
            {caseData.status === 'OPEN' && (
              <button
                type="button"
                onClick={() => handleStatusTransition('IN_REVIEW')}
                className="w-full py-2.5 bg-amber-600 hover:bg-amber-500 text-white rounded-lg text-xs font-semibold transition-colors flex items-center justify-center gap-2"
              >
                <Clock size={16} />
                <span>Move to Active Investigation (IN_REVIEW)</span>
              </button>
            )}

            {/* IN_REVIEW -> RESOLVED */}
            {(caseData.status === 'IN_REVIEW' || caseData.status === 'OPEN') && (
              <div className="p-4 rounded-xl bg-slate-800/40 border border-slate-800 space-y-3">
                <label className="block text-xs font-semibold text-slate-300">
                  Resolution Summary (Mandatory for RESOLVED):
                </label>
                <textarea
                  value={resolutionSummary}
                  onChange={(e) => setResolutionSummary(e.target.value)}
                  placeholder="Document the operational resolution, verified documents, or risk sign-off..."
                  rows={3}
                  className="w-full p-2.5 bg-slate-900 border border-slate-700 rounded-lg text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                />
                <button
                  type="button"
                  onClick={() => handleStatusTransition('RESOLVED')}
                  className="w-full py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-semibold transition-colors flex items-center justify-center gap-2"
                >
                  <CheckCircle size={15} />
                  <span>Mark Case as RESOLVED</span>
                </button>
              </div>
            )}

            {/* RESOLVED -> REOPENED (Admin Only) */}
            {caseData.status === 'RESOLVED' && (
              <div className="p-4 rounded-xl bg-slate-800/40 border border-slate-800 space-y-3">
                <div className="text-xs text-emerald-400 font-semibold flex items-center gap-2">
                  <CheckCircle size={16} />
                  <span>Case is Currently Resolved</span>
                </div>
                {caseData.resolutionSummary && (
                  <p className="text-xs text-slate-300 bg-slate-900/60 p-3 rounded border border-slate-800">
                    <strong>Resolution Record:</strong> {caseData.resolutionSummary}
                  </p>
                )}

                {isAdmin ? (
                  <div className="pt-2 border-t border-slate-700/60 space-y-2">
                    <label className="block text-xs font-semibold text-purple-300">
                      Administrative Reopen Justification:
                    </label>
                    <textarea
                      value={reopenJustification}
                      onChange={(e) => setReopenJustification(e.target.value)}
                      placeholder="Audit justification for reopening resolved case..."
                      rows={2}
                      className="w-full p-2 bg-slate-900 border border-slate-700 rounded-lg text-xs text-slate-100"
                    />
                    <button
                      type="button"
                      onClick={() => handleStatusTransition('REOPENED')}
                      className="w-full py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-lg text-xs font-semibold transition-colors flex items-center justify-center gap-2"
                    >
                      <RotateCcw size={15} />
                      <span>Reopen Case (Admin Privilege)</span>
                    </button>
                  </div>
                ) : (
                  <p className="text-[11px] text-slate-500 italic">
                    Only users with ROLE_ADMIN are authorized to reopen a resolved operational case.
                  </p>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Investigation Notes & Timeline */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-sm flex flex-col justify-between">
          <div>
            <h2 className="text-sm font-bold text-white uppercase tracking-wider flex items-center gap-2 mb-4">
              <MessageSquare size={18} className="text-sky-400" />
              <span>Investigation Journal & Audit Trail</span>
            </h2>

            {/* Chronological Timeline */}
            <div className="space-y-3 max-h-96 overflow-y-auto pr-2">
              {timeline.length === 0 ? (
                <p className="text-xs text-slate-500 italic">No notes or events logged yet.</p>
              ) : (
                timeline.map((evt, idx) => (
                  <div key={idx} className="p-3 bg-slate-800/40 border border-slate-800 rounded-xl text-xs space-y-1">
                    <div className="flex items-center justify-between text-[10px] text-slate-400">
                      <span className="font-semibold text-sky-400">@{evt.actorUsername}</span>
                      <span className="font-mono">{new Date(evt.timestamp).toLocaleString()}</span>
                    </div>
                    <p className="text-slate-200 leading-relaxed font-sans">{evt.description}</p>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Add Note Form */}
          <form onSubmit={handleAddNote} className="mt-4 pt-4 border-t border-slate-800">
            <div className="relative">
              <input
                type="text"
                value={newNote}
                onChange={(e) => setNewNote(e.target.value)}
                placeholder="Log internal investigation finding or note..."
                className="w-full pl-3 pr-10 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-sky-500"
              />
              <button
                type="submit"
                disabled={submittingNote || !newNote.trim()}
                className="absolute right-2 top-2 p-1 text-sky-400 hover:text-sky-300 disabled:opacity-40"
              >
                <Send size={16} />
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
