import React from 'react';

interface SeverityBadgeProps {
  severity: string;
}

export const SeverityBadge: React.FC<SeverityBadgeProps> = ({ severity }) => {
  const normalized = (severity || '').toUpperCase();

  switch (normalized) {
    case 'CRITICAL':
      return <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-red-950 text-red-400 border border-red-700/60 animate-pulse">CRITICAL</span>;
    case 'HIGH':
      return <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-orange-950 text-orange-400 border border-orange-800">HIGH</span>;
    case 'MEDIUM':
      return <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-amber-950/70 text-amber-300 border border-amber-800/80">MEDIUM</span>;
    case 'LOW':
      return <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-slate-800 text-slate-300 border border-slate-700">LOW</span>;
    default:
      return <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-slate-800 text-slate-400">{severity}</span>;
  }
};
