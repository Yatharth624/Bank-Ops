import React, { useEffect, useState } from 'react';
import { apiClient } from '../api/client';
import { ValidationRule } from '../types';
import { SeverityBadge } from '../components/SeverityBadge';
import {
  Sliders,
  CheckCircle,
  XCircle,
  Edit2,
  Plus,
  X,
  AlertCircle,
} from 'lucide-react';

export const RuleConfig: React.FC = () => {
  const [rules, setRules] = useState<ValidationRule[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedRule, setSelectedRule] = useState<ValidationRule | null>(null);
  const [editParameters, setEditParameters] = useState('');
  const [editActive, setEditActive] = useState(true);
  const [saving, setSaving] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  const fetchRules = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get<any>('/admin/rules');
      if (res.data.success) {
        setRules(res.data.data);
      }
    } catch (err) {
      console.error('Failed to load rules', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRules();
  }, []);

  const openEditModal = (rule: ValidationRule) => {
    setSelectedRule(rule);
    setEditParameters(rule.parameters || '');
    setEditActive(rule.active);
  };

  const handleSaveRule = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedRule) return;

    setSaving(true);
    setStatusMessage(null);
    try {
      const res = await apiClient.patch(`/admin/rules/${selectedRule.id}`, {
        parameters: editParameters,
        active: editActive,
      });
      if (res.data.success) {
        setStatusMessage('Rule configuration updated successfully');
        setSelectedRule(null);
        fetchRules();
      }
    } catch (err: any) {
      setStatusMessage(err.response?.data?.message || 'Failed to update rule');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-white">Validation Rules Engine Configuration</h1>
        <p className="text-xs text-slate-400 mt-1">
          Configure rule thresholds, activate/deactivate triggers, and review categorization profiles.
        </p>
      </div>

      {statusMessage && (
        <div className="p-3 bg-sky-950/60 border border-sky-800 text-sky-300 text-xs rounded-xl flex items-center gap-2">
          <CheckCircle size={16} />
          <span>{statusMessage}</span>
        </div>
      )}

      {/* Rules Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl shadow-sm overflow-hidden">
        <table className="w-full text-left text-xs">
          <thead className="bg-slate-800/80 text-slate-400 font-semibold uppercase tracking-wider border-b border-slate-800">
            <tr>
              <th className="py-3 px-4">Rule Code</th>
              <th className="py-3 px-4">Rule Name</th>
              <th className="py-3 px-4">Category</th>
              <th className="py-3 px-4">Severity</th>
              <th className="py-3 px-4">Parameters (JSON)</th>
              <th className="py-3 px-4">Status</th>
              <th className="py-3 px-4 text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 text-slate-300">
            {loading ? (
              <tr>
                <td colSpan={7} className="py-8 text-center text-slate-400">
                  Loading operational rules...
                </td>
              </tr>
            ) : rules.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-8 text-center text-slate-500">
                  No validation rules registered.
                </td>
              </tr>
            ) : (
              rules.map((rule) => (
                <tr key={rule.id} className="hover:bg-slate-800/40 transition-colors">
                  <td className="py-3 px-4 font-mono font-semibold text-sky-400">{rule.ruleCode}</td>
                  <td className="py-3 px-4 font-semibold text-white">{rule.ruleName}</td>
                  <td className="py-3 px-4">{rule.category.replace(/_/g, ' ')}</td>
                  <td className="py-3 px-4">
                    <SeverityBadge severity={rule.severity} />
                  </td>
                  <td className="py-3 px-4 font-mono text-[11px] text-slate-400 max-w-xs truncate">
                    {rule.parameters || 'None'}
                  </td>
                  <td className="py-3 px-4">
                    {rule.active ? (
                      <span className="inline-flex items-center gap-1 text-[11px] text-emerald-400">
                        <CheckCircle size={14} /> Active
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 text-[11px] text-slate-500">
                        <XCircle size={14} /> Inactive
                      </span>
                    )}
                  </td>
                  <td className="py-3 px-4 text-right">
                    <button
                      onClick={() => openEditModal(rule)}
                      className="p-1.5 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-colors"
                      title="Edit Rule Configuration"
                    >
                      <Edit2 size={14} />
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Edit Rule Modal */}
      {selectedRule && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 shadow-2xl relative">
            <button
              onClick={() => setSelectedRule(null)}
              className="absolute top-4 right-4 text-slate-400 hover:text-white"
            >
              <X size={18} />
            </button>
            <h3 className="text-lg font-bold text-white mb-1">Edit Validation Rule</h3>
            <p className="text-xs font-mono text-sky-400 mb-4">{selectedRule.ruleCode}</p>

            <form onSubmit={handleSaveRule} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Parameters Configuration (JSON)
                </label>
                <textarea
                  value={editParameters}
                  onChange={(e) => setEditParameters(e.target.value)}
                  rows={4}
                  className="w-full p-2.5 bg-slate-800 border border-slate-700 rounded-lg text-xs font-mono text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-sky-500"
                  placeholder='{"threshold": 50000.00}'
                />
              </div>

              <div className="flex items-center gap-3">
                <input
                  type="checkbox"
                  id="activeCheck"
                  checked={editActive}
                  onChange={(e) => setEditActive(e.target.checked)}
                  className="rounded bg-slate-800 border-slate-700 text-sky-600 focus:ring-sky-500"
                />
                <label htmlFor="activeCheck" className="text-xs text-slate-300">
                  Rule Trigger Active
                </label>
              </div>

              <button
                type="submit"
                disabled={saving}
                className="w-full py-2 bg-sky-600 hover:bg-sky-500 disabled:opacity-50 text-white rounded-lg text-xs font-semibold transition-colors"
              >
                {saving ? 'Saving Changes...' : 'Save Configuration'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
