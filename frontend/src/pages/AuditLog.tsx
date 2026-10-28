import React, { useEffect, useState } from 'react';
import { apiClient } from '../api/client';
import { AuditEvent } from '../types';
import {
  History,
  Search,
  Filter,
  ShieldAlert,
  ChevronLeft,
  ChevronRight,
  Info,
} from 'lucide-react';

export const AuditLog: React.FC = () => {
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  // Filters
  const [entityType, setEntityType] = useState('');
  const [action, setAction] = useState('');
  const [actorUsername, setActorUsername] = useState('');

  const fetchAuditEvents = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      params.append('page', page.toString());
      params.append('size', '20');
      if (entityType) params.append('entityType', entityType);
      if (action) params.append('action', action);
      if (actorUsername) params.append('actorUsername', actorUsername);

      const res = await apiClient.get<any>(`/audit-events?${params.toString()}`);
      if (res.data.success) {
        setEvents(res.data.data.content);
        setTotalPages(res.data.data.totalPages);
        setTotalElements(res.data.data.totalElements);
      }
    } catch (err) {
      console.error('Failed to load audit trail', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAuditEvents();
  }, [page, entityType, action]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchAuditEvents();
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-white">Immutable Audit Trail</h1>
        <p className="text-xs text-slate-400 mt-1">
          Chronological, append-only operational events log recording authentication, batch imports, case modifications, and administration.
        </p>
      </div>

      {/* Security Note regarding Append-Only guarantees */}
      <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-800 flex items-start gap-3 text-xs text-slate-300">
        <Info size={18} className="text-sky-400 shrink-0 mt-0.5" />
        <p className="leading-relaxed">
          <strong>Security Disclosure:</strong> Application-level access control enforces strict append-only constraints across all user roles and REST APIs. However, application-level controls alone do not make records mathematically tamper-evident against privileged database administrators (DBAs) with direct SQL access. For regulatory non-repudiation, external immutable storage WORM logs should be coupled in production.
        </p>
      </div>

      {/* Filter Bar */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 flex flex-col md:flex-row gap-4 justify-between items-center">
        <form onSubmit={handleSearchSubmit} className="flex-1 w-full md:max-w-md relative">
          <input
            type="text"
            value={actorUsername}
            onChange={(e) => setActorUsername(e.target.value)}
            placeholder="Filter by Actor Username..."
            className="w-full pl-9 pr-3 py-1.5 bg-slate-800 border border-slate-700 rounded-lg text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-sky-500"
          />
          <Search size={15} className="absolute left-3 top-2.5 text-slate-500" />
        </form>

        <div className="flex items-center gap-3 w-full md:w-auto">
          <select
            value={entityType}
            onChange={(e) => {
              setEntityType(e.target.value);
              setPage(0);
            }}
            className="px-2.5 py-1.5 bg-slate-800 border border-slate-700 rounded-lg text-xs text-slate-200"
          >
            <option value="">All Entity Types</option>
            <option value="AUTHENTICATION">Authentication</option>
            <option value="EXCEPTION_CASE">Exception Case</option>
            <option value="TRANSACTION_BATCH">Transaction Batch</option>
            <option value="VALIDATION_RULE">Validation Rule</option>
            <option value="USER">User Account</option>
          </select>
        </div>
      </div>

      {/* Audit Events Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-800/80 text-slate-400 font-semibold uppercase tracking-wider border-b border-slate-800">
              <tr>
                <th className="py-3 px-4">Timestamp</th>
                <th className="py-3 px-4">Actor</th>
                <th className="py-3 px-4">Action</th>
                <th className="py-3 px-4">Entity</th>
                <th className="py-3 px-4">Correlation ID</th>
                <th className="py-3 px-4">Details</th>
                <th className="py-3 px-4 text-right">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-mono text-slate-300">
              {loading ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-400 font-sans">
                    Loading audit trail...
                  </td>
                </tr>
              ) : events.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-500 font-sans">
                    No audit records found.
                  </td>
                </tr>
              ) : (
                events.map((evt) => (
                  <tr key={evt.id} className="hover:bg-slate-800/40 transition-colors">
                    <td className="py-3 px-4 text-slate-400 text-[11px]">
                      {new Date(evt.createdAt).toLocaleString()}
                    </td>
                    <td className="py-3 px-4">
                      <span className="font-semibold text-sky-400">@{evt.actorUsername}</span>
                      <span className="block text-[10px] text-slate-500">{evt.actorRole}</span>
                    </td>
                    <td className="py-3 px-4 font-bold text-slate-200">{evt.action}</td>
                    <td className="py-3 px-4 text-slate-300">{evt.entityType}</td>
                    <td className="py-3 px-4 text-slate-400 text-[11px] truncate max-w-[120px]">
                      {evt.correlationId || '—'}
                    </td>
                    <td className="py-3 px-4 font-sans text-slate-300 max-w-xs truncate" title={evt.details}>
                      {evt.details}
                    </td>
                    <td className="py-3 px-4 text-right font-sans">
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold ${
                          evt.status === 'SUCCESS'
                            ? 'bg-emerald-950 text-emerald-400 border border-emerald-800'
                            : 'bg-rose-950 text-rose-400 border border-rose-800'
                        }`}
                      >
                        {evt.status}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        <div className="p-4 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
          <span>
            Showing page <strong>{page + 1}</strong> of <strong>{totalPages || 1}</strong> ({totalElements} total entries)
          </span>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="p-1.5 rounded bg-slate-800 border border-slate-700 hover:bg-slate-700 disabled:opacity-40 transition-colors"
            >
              <ChevronLeft size={16} />
            </button>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="p-1.5 rounded bg-slate-800 border border-slate-700 hover:bg-slate-700 disabled:opacity-40 transition-colors"
            >
              <ChevronRight size={16} />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
