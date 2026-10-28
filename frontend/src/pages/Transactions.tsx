import React, { useEffect, useState } from 'react';
import { apiClient } from '../api/client';
import { Transaction, PagedResponse, ImportResult } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import {
  Search,
  UploadCloud,
  FileSpreadsheet,
  AlertCircle,
  CheckCircle,
  Eye,
  X,
  ChevronLeft,
  ChevronRight,
  Filter,
} from 'lucide-react';

export const Transactions: React.FC = () => {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  // Filters
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [channelFilter, setChannelFilter] = useState('');

  // Selected Transaction Modal
  const [selectedTx, setSelectedTx] = useState<Transaction | null>(null);

  // Import Modal & State
  const [showImportModal, setShowImportModal] = useState(false);
  const [importFile, setImportFile] = useState<File | null>(null);
  const [importLoading, setImportLoading] = useState(false);
  const [importResult, setImportResult] = useState<ImportResult | null>(null);
  const [importError, setImportError] = useState<string | null>(null);

  const fetchTransactions = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      params.append('page', page.toString());
      params.append('size', '15');
      if (search) params.append('search', search);
      if (statusFilter) params.append('status', statusFilter);
      if (channelFilter) params.append('channel', channelFilter);

      const res = await apiClient.get<any>(`/transactions?${params.toString()}`);
      if (res.data.success) {
        setTransactions(res.data.data.content);
        setTotalPages(res.data.data.totalPages);
        setTotalElements(res.data.data.totalElements);
      }
    } catch (err) {
      console.error('Failed to fetch transactions', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, [page, statusFilter, channelFilter]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchTransactions();
  };

  const handleImportSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!importFile) return;

    setImportLoading(true);
    setImportError(null);
    setImportResult(null);

    const formData = new FormData();
    formData.append('file', importFile);

    const idempotencyKey = 'IMP-' + Date.now() + '-' + Math.random().toString(36).substring(2, 7);

    try {
      const res = await apiClient.post('/transactions/import', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
          'X-Idempotency-Key': idempotencyKey,
        },
      });
      if (res.data.success) {
        setImportResult(res.data.data);
        fetchTransactions();
      } else {
        setImportError(res.data.message || 'Import failed');
      }
    } catch (err: any) {
      setImportError(err.response?.data?.message || 'Error executing CSV ingestion');
    } finally {
      setImportLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header and Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Synthetic Transactions</h1>
          <p className="text-xs text-slate-400 mt-1">
            Browse, filter, and ingest synthetic transactions with row-level error validation.
          </p>
        </div>

        <button
          onClick={() => {
            setShowImportModal(true);
            setImportResult(null);
            setImportError(null);
          }}
          className="flex items-center gap-2 px-4 py-2 bg-sky-600 hover:bg-sky-500 text-white rounded-lg text-sm font-semibold shadow-md shadow-sky-900/30 transition-all"
        >
          <UploadCloud size={16} />
          <span>Ingest CSV Batch</span>
        </button>
      </div>

      {/* Filter Bar */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 flex flex-col md:flex-row gap-4 justify-between items-center">
        <form onSubmit={handleSearchSubmit} className="flex-1 w-full md:max-w-md relative">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search Reference, Account, Destination..."
            className="w-full pl-9 pr-3 py-1.5 bg-slate-800 border border-slate-700 rounded-lg text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-sky-500"
          />
          <Search size={15} className="absolute left-3 top-2.5 text-slate-500" />
        </form>

        <div className="flex items-center gap-3 w-full md:w-auto">
          <div className="flex items-center gap-2">
            <Filter size={14} className="text-slate-400" />
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              className="px-2.5 py-1.5 bg-slate-800 border border-slate-700 rounded-lg text-xs text-slate-200"
            >
              <option value="">All Statuses</option>
              <option value="COMPLETED">Completed</option>
              <option value="PENDING">Pending</option>
              <option value="FAILED">Failed</option>
              <option value="REJECTED">Rejected</option>
            </select>
          </div>

          <select
            value={channelFilter}
            onChange={(e) => {
              setChannelFilter(e.target.value);
              setPage(0);
            }}
            className="px-2.5 py-1.5 bg-slate-800 border border-slate-700 rounded-lg text-xs text-slate-200"
          >
            <option value="">All Channels</option>
            <option value="WEB">Web</option>
            <option value="MOBILE">Mobile</option>
            <option value="BRANCH">Branch</option>
            <option value="BATCH_API">Batch API</option>
          </select>
        </div>
      </div>

      {/* Transactions Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-800/80 text-slate-400 font-semibold uppercase tracking-wider border-b border-slate-800">
              <tr>
                <th className="py-3 px-4">External Reference</th>
                <th className="py-3 px-4">Account</th>
                <th className="py-3 px-4">Amount</th>
                <th className="py-3 px-4">Type</th>
                <th className="py-3 px-4">Channel</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Timestamp</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-mono text-slate-300">
              {loading ? (
                <tr>
                  <td colSpan={8} className="py-8 text-center text-slate-400 font-sans">
                    Loading telemetry records...
                  </td>
                </tr>
              ) : transactions.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-8 text-center text-slate-500 font-sans">
                    No transactions match current filters.
                  </td>
                </tr>
              ) : (
                transactions.map((tx) => (
                  <tr key={tx.id} className="hover:bg-slate-800/40 transition-colors">
                    <td className="py-3 px-4 font-semibold text-sky-400">{tx.externalReference}</td>
                    <td className="py-3 px-4">{tx.accountReference}</td>
                    <td className="py-3 px-4 font-sans font-semibold text-white">
                      {tx.currency} {Number(tx.amount).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </td>
                    <td className="py-3 px-4 font-sans">{tx.transactionType}</td>
                    <td className="py-3 px-4 font-sans text-slate-400">{tx.channel}</td>
                    <td className="py-3 px-4 font-sans">
                      <StatusBadge status={tx.status} />
                    </td>
                    <td className="py-3 px-4 text-slate-400 text-[11px]">
                      {new Date(tx.transactionTimestamp).toLocaleString()}
                    </td>
                    <td className="py-3 px-4 text-right font-sans">
                      <button
                        onClick={() => setSelectedTx(tx)}
                        className="p-1 rounded text-slate-400 hover:text-sky-400 hover:bg-slate-800 transition-colors"
                        title="View Details"
                      >
                        <Eye size={16} />
                      </button>
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
            Showing page <strong>{page + 1}</strong> of <strong>{totalPages || 1}</strong> ({totalElements} total records)
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

      {/* Transaction Details Modal */}
      {selectedTx && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-lg w-full p-6 shadow-2xl relative">
            <button
              onClick={() => setSelectedTx(null)}
              className="absolute top-4 right-4 text-slate-400 hover:text-white"
            >
              <X size={18} />
            </button>
            <h3 className="text-lg font-bold text-white mb-1">Transaction Details</h3>
            <p className="text-xs font-mono text-sky-400 mb-4">{selectedTx.externalReference}</p>

            <div className="grid grid-cols-2 gap-3 text-xs bg-slate-800/50 p-4 rounded-xl border border-slate-800 mb-4 font-mono">
              <div>
                <span className="text-slate-500 block text-[10px] uppercase font-sans">Account Ref:</span>
                <span className="text-slate-200">{selectedTx.accountReference}</span>
              </div>
              <div>
                <span className="text-slate-500 block text-[10px] uppercase font-sans">Amount:</span>
                <span className="text-white font-bold">{selectedTx.currency} {Number(selectedTx.amount).toFixed(2)}</span>
              </div>
              <div>
                <span className="text-slate-500 block text-[10px] uppercase font-sans">Type:</span>
                <span className="text-slate-200">{selectedTx.transactionType}</span>
              </div>
              <div>
                <span className="text-slate-500 block text-[10px] uppercase font-sans">Channel:</span>
                <span className="text-slate-200">{selectedTx.channel}</span>
              </div>
              <div>
                <span className="text-slate-500 block text-[10px] uppercase font-sans">Status:</span>
                <StatusBadge status={selectedTx.status} />
              </div>
              <div>
                <span className="text-slate-500 block text-[10px] uppercase font-sans">Destination:</span>
                <span className="text-slate-200">{selectedTx.merchantOrDestination || '—'}</span>
              </div>
            </div>

            {selectedTx.failureReason && (
              <div className="p-3 rounded-lg bg-rose-950/40 border border-rose-800/80 text-rose-300 text-xs mb-4">
                <strong>Failure Reason:</strong> {selectedTx.failureReason}
              </div>
            )}

            <button
              onClick={() => setSelectedTx(null)}
              className="w-full py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-lg text-xs font-semibold transition-colors"
            >
              Close
            </button>
          </div>
        </div>
      )}

      {/* CSV Ingestion Modal */}
      {showImportModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-xl w-full p-6 shadow-2xl relative">
            <button
              onClick={() => setShowImportModal(false)}
              className="absolute top-4 right-4 text-slate-400 hover:text-white"
            >
              <X size={18} />
            </button>
            <h3 className="text-lg font-bold text-white mb-1">Batch CSV Transaction Ingestion</h3>
            <p className="text-xs text-slate-400 mb-6">
              Upload synthetic transactions for rule evaluation, duplicate reference detection, and operational case triage.
            </p>

            <form onSubmit={handleImportSubmit} className="space-y-4">
              <div className="border-2 border-dashed border-slate-700 rounded-xl p-6 text-center hover:border-sky-500 transition-colors">
                <FileSpreadsheet size={32} className="mx-auto text-sky-400 mb-2" />
                <label className="cursor-pointer">
                  <span className="text-xs font-semibold text-sky-400 hover:text-sky-300">
                    Click to select CSV file
                  </span>
                  <input
                    type="file"
                    accept=".csv"
                    onChange={(e) => setImportFile(e.target.files?.[0] || null)}
                    className="hidden"
                  />
                </label>
                {importFile && (
                  <p className="mt-2 text-xs font-mono text-emerald-400">
                    Selected: {importFile.name} ({(importFile.size / 1024).toFixed(1)} KB)
                  </p>
                )}
              </div>

              {importError && (
                <div className="p-3 rounded-lg bg-rose-950/50 border border-rose-800 text-rose-300 text-xs">
                  {importError}
                </div>
              )}

              {importResult && (
                <div className="space-y-3">
                  <div className="grid grid-cols-3 gap-2 text-center text-xs">
                    <div className="p-2 rounded bg-emerald-950/60 border border-emerald-800 text-emerald-300">
                      <div className="font-bold text-base">{importResult.successfullyImportedCount}</div>
                      <div className="text-[10px]">Imported</div>
                    </div>
                    <div className="p-2 rounded bg-amber-950/60 border border-amber-800 text-amber-300">
                      <div className="font-bold text-base">{importResult.duplicateSkippedCount}</div>
                      <div className="text-[10px]">Duplicates Skipped</div>
                    </div>
                    <div className="p-2 rounded bg-rose-950/60 border border-rose-800 text-rose-300">
                      <div className="font-bold text-base">{importResult.exceptionCasesTriggeredCount}</div>
                      <div className="text-[10px]">Exceptions Raised</div>
                    </div>
                  </div>

                  {importResult.rowErrors.length > 0 && (
                    <div className="max-h-32 overflow-y-auto p-2 bg-slate-950 rounded border border-slate-800 text-[11px] font-mono text-rose-400 space-y-1">
                      {importResult.rowErrors.map((err, i) => (
                        <div key={i}>
                          Row {err.rowNumber} [{err.externalReference}]: {err.errorMessage}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}

              <button
                type="submit"
                disabled={!importFile || importLoading}
                className="w-full py-2.5 bg-sky-600 hover:bg-sky-500 disabled:opacity-50 text-white rounded-lg text-xs font-semibold transition-colors"
              >
                {importLoading ? 'Validating and Ingesting...' : 'Start Ingestion'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
