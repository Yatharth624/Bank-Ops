import React, { useEffect, useState } from 'react';
import { apiClient } from '../api/client';
import { DashboardSummary, TrendPoint } from '../types';
import { StatCard } from '../components/StatCard';
import {
  ArrowLeftRight,
  CheckCircle2,
  AlertOctagon,
  AlertTriangle,
  Clock,
  Percent,
  RefreshCw,
  FolderOpen,
  PieChart as PieIcon,
  BarChart2,
} from 'lucide-react';
import {
  AreaChart,
  Area,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';

export const Dashboard: React.FC = () => {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [trends, setTrends] = useState<TrendPoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [trendDays, setTrendDays] = useState(7);
  const [error, setError] = useState<string | null>(null);

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [sumRes, trendRes] = await Promise.all([
        apiClient.get('/dashboard/summary'),
        apiClient.get(`/dashboard/trends?days=${trendDays}`),
      ]);

      if (sumRes.data.success) setSummary(sumRes.data.data);
      if (trendRes.data.success) setTrends(trendRes.data.data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch operational metrics');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, [trendDays]);

  const severityData = summary?.casesBySeverity
    ? Object.entries(summary.casesBySeverity).map(([name, count]) => ({ name, count }))
    : [];

  const categoryData = summary?.exceptionsByCategory
    ? Object.entries(summary.exceptionsByCategory).map(([name, value]) => ({
        name: name.replace(/_/g, ' '),
        value,
      }))
    : [];

  const SEVERITY_COLORS: Record<string, string> = {
    CRITICAL: '#ef4444',
    HIGH: '#f97316',
    MEDIUM: '#f59e0b',
    LOW: '#64748b',
  };

  const CATEGORY_COLORS = ['#0284c7', '#6366f1', '#ec4899', '#14b8a6', '#f59e0b'];

  return (
    <div className="space-y-8">
      {/* Page Title & Refresh */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Operations Control Dashboard</h1>
          <p className="text-xs text-slate-400 mt-1">
            Real-time transaction telemetry, exception triage queue, and resolution metrics.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <select
            value={trendDays}
            onChange={(e) => setTrendDays(Number(e.target.value))}
            className="px-3 py-1.5 bg-slate-800 border border-slate-700 rounded-lg text-xs text-slate-200 focus:outline-none focus:ring-1 focus:ring-sky-500"
          >
            <option value={7}>Last 7 Days</option>
            <option value={14}>Last 14 Days</option>
            <option value={30}>Last 30 Days</option>
          </select>

          <button
            onClick={fetchDashboardData}
            disabled={loading}
            className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-lg text-xs text-slate-200 transition-colors"
          >
            <RefreshCw size={14} className={loading ? 'animate-spin text-sky-400' : ''} />
            <span>Sync</span>
          </button>
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-950/40 border border-rose-800 text-rose-300 text-sm">
          {error}
        </div>
      )}

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Transactions"
          value={summary?.totalTransactions ?? '—'}
          subtitle="Processed synthetic volume"
          icon={<ArrowLeftRight size={20} />}
        />
        <StatCard
          title="Successful Volume"
          value={summary?.successfulTransactions ?? '—'}
          subtitle="Completed clearing state"
          icon={<CheckCircle2 size={20} className="text-emerald-400" />}
        />
        <StatCard
          title="Failed / Rejected"
          value={summary?.failedTransactions ?? '—'}
          subtitle="Processing exceptions"
          icon={<AlertOctagon size={20} className="text-rose-400" />}
        />
        <StatCard
          title="Exception Rate"
          value={`${summary?.exceptionRatePercentage ?? 0}%`}
          subtitle="(Total Cases ÷ Total Tx) × 100"
          icon={<Percent size={20} className="text-amber-400" />}
        />
      </div>

      {/* Secondary Operations Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard
          title="Open Triage Cases"
          value={summary?.openExceptions ?? '—'}
          subtitle="Awaiting analyst assignment"
          icon={<FolderOpen size={20} className="text-blue-400" />}
        />
        <StatCard
          title="Under Active Review"
          value={summary?.inReviewExceptions ?? '—'}
          subtitle="Assigned and in review"
          icon={<AlertTriangle size={20} className="text-amber-400" />}
        />
        <StatCard
          title="Avg Resolution Time"
          value={`${summary?.averageResolutionTimeHours ?? 0} hrs`}
          subtitle="Mean resolution duration"
          icon={<Clock size={20} className="text-emerald-400" />}
        />
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Trend Area Chart (2 Cols) */}
        <div className="lg:col-span-2 bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-sm">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-sm font-semibold text-white tracking-wide">
                Volume & Exception Trends
              </h2>
              <p className="text-xs text-slate-400 mt-0.5">
                Completed vs failed transactions and triggered operational exceptions.
              </p>
            </div>
            <BarChart2 size={18} className="text-slate-500" />
          </div>

          <div className="h-72 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={trends} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorTotal" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#0284c7" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#0284c7" stopOpacity={0.0} />
                  </linearGradient>
                  <linearGradient id="colorExceptions" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#f43f5e" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#f43f5e" stopOpacity={0.0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="date" stroke="#64748b" fontSize={11} />
                <YAxis stroke="#64748b" fontSize={11} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '8px' }}
                  labelStyle={{ color: '#f8fafc', fontWeight: 600, fontSize: '12px' }}
                />
                <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '10px' }} />
                <Area type="monotone" dataKey="total" stroke="#0284c7" fillOpacity={1} fill="url(#colorTotal)" name="Total Transactions" />
                <Area type="monotone" dataKey="exceptions" stroke="#f43f5e" fillOpacity={1} fill="url(#colorExceptions)" name="Exception Cases" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Category Breakdown Pie Chart (1 Col) */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-sm flex flex-col justify-between">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-sm font-semibold text-white tracking-wide">
                Exceptions by Category
              </h2>
              <p className="text-xs text-slate-400 mt-0.5">Distribution across rule categories.</p>
            </div>
            <PieIcon size={18} className="text-slate-500" />
          </div>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={categoryData}
                  cx="50%"
                  cy="50%"
                  innerRadius={50}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {categoryData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={CATEGORY_COLORS[index % CATEGORY_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '8px' }}
                />
                <Legend wrapperStyle={{ fontSize: '11px' }} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Severity Breakdown Bar Chart */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-sm">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-sm font-semibold text-white tracking-wide">
              Active Exception Severity Profile
            </h2>
            <p className="text-xs text-slate-400 mt-0.5">
              Risk exposure categorization across open and in-review cases.
            </p>
          </div>
        </div>

        <div className="h-56 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={severityData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
              <XAxis dataKey="name" stroke="#64748b" fontSize={11} />
              <YAxis stroke="#64748b" fontSize={11} />
              <Tooltip
                contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '8px' }}
              />
              <Bar dataKey="count" name="Case Count" radius={[4, 4, 0, 0]}>
                {severityData.map((entry) => (
                  <Cell key={`bar-${entry.name}`} fill={SEVERITY_COLORS[entry.name] || '#38bdf8'} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
};
