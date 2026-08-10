import React from 'react';
import { clsx } from 'clsx';

interface GlassCardProps {
  children: React.ReactNode;
  className?: string;
  hoverEffect?: boolean;
  onClick?: () => void;
}

export const GlassCard: React.FC<GlassCardProps> = ({ children, className, hoverEffect = false, onClick }) => {
  return (
    <div
      onClick={onClick}
      className={clsx(
        "bg-[#0e060c]/75 backdrop-blur-xl border border-red-500/20 rounded-2xl p-6 shadow-2xl relative overflow-hidden transition-all duration-300",
        hoverEffect && "hover:border-red-500/50 hover:bg-[#180914]/85 hover:-translate-y-1 hover:shadow-[0_12px_35px_-5px_rgba(255,0,60,0.3)] cursor-pointer",
        className
      )}
    >
      {children}
    </div>
  );
};
