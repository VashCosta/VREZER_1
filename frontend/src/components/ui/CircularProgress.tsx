import React, { useEffect, useState } from 'react';

interface CircularProgressProps {
  percentage: number;
  label?: string;
  subLabel?: string;
  size?: number;
  strokeWidth?: number;
  color?: string;
  className?: string;
}

export const CircularProgress: React.FC<CircularProgressProps> = ({
  percentage,
  label,
  subLabel,
  size = 120,
  strokeWidth = 10,
  color = "#ff003c",
  className = ""
}) => {
  const [progress, setProgress] = useState(0);
  
  useEffect(() => {
    // Animate progress
    const timer = setTimeout(() => {
      setProgress(percentage);
    }, 100);
    return () => clearTimeout(timer);
  }, [percentage]);

  const radius = (size - strokeWidth) / 2;
  const circumference = radius * 2 * Math.PI;
  const offset = circumference - (progress / 100) * circumference;

  return (
    <div className={`relative flex flex-col items-center justify-center ${className}`} style={{ width: size, height: size }}>
      <svg className="transform -rotate-90" width={size} height={size}>
        {/* Background Circle */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="transparent"
          stroke="#450a0a" // dark red background
          strokeWidth={strokeWidth}
        />
        {/* Progress Circle */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="transparent"
          stroke={color}
          strokeWidth={strokeWidth}
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="round"
          className="transition-all duration-1000 ease-out"
          style={{ filter: `drop-shadow(0 0 8px ${color})` }}
        />
      </svg>
      <div className="absolute flex flex-col items-center justify-center text-center">
        {label && <span className={`font-black text-white ${size < 80 ? 'text-xs' : 'text-3xl'}`}>{label}</span>}
        {subLabel && <span className={`font-bold tracking-widest text-slate-400 mt-1 uppercase ${size < 80 ? 'text-[6px]' : 'text-[9px]'}`}>{subLabel}</span>}
      </div>
    </div>
  );
};
