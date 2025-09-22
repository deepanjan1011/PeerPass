'use client';

interface ProgressBarProps {
  progress: number; // 0-100
  speed: number; // MB/s
  eta: number; // seconds
  fileName: string;
  fileSize: number; // bytes
  transferred: number; // bytes
}

export default function ProgressBar({ 
  progress, 
  speed, 
  eta, 
  fileName, 
  fileSize, 
  transferred 
}: ProgressBarProps) {
  const formatBytes = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B';
    else if (bytes < 1024 * 1024) return Math.round(bytes / 1024) + ' KB';
    else if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    else return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB';
  };

  const formatTime = (seconds: number) => {
    if (seconds < 60) return `${Math.round(seconds)}s`;
    else if (seconds < 3600) return `${Math.round(seconds / 60)}m ${Math.round(seconds % 60)}s`;
    else return `${Math.round(seconds / 3600)}h ${Math.round((seconds % 3600) / 60)}m`;
  };

  const formatSpeed = (mbps: number) => {
    if (mbps < 1) return `${(mbps * 1024).toFixed(0)} KB/s`;
    return `${mbps.toFixed(1)} MB/s`;
  };

  return (
    <div className="w-full space-y-3">
      {/* File info */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <svg className="w-5 h-5 text-primary flex-shrink-0" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <div className="min-w-0 flex-1">
            <p className="font-medium text-foreground truncate">
              {fileName}
            </p>
            <p className="text-sm text-muted-foreground">
              {formatBytes(transferred)} of {formatBytes(fileSize)}
            </p>
          </div>
        </div>
        <div className="text-right">
          <p className="text-lg font-semibold text-primary">
            {progress.toFixed(1)}%
          </p>
        </div>
      </div>

      {/* Progress bar */}
      <div className="progress-container">
        <div 
          className={`progress-bar ${progress > 0 && progress < 100 ? 'progress-bar-animated' : ''}`}
          style={{ width: `${progress}%` }}
        />
      </div>

      {/* Transfer stats */}
      <div className="transfer-stats">
        <div className="flex items-center space-x-2">
          <svg className="w-4 h-4 text-primary" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
          <span className="speed-indicator">
            {formatSpeed(speed)}
          </span>
        </div>
        
        <div className="flex items-center space-x-2">
          <svg className="w-4 h-4 text-muted-foreground" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span className="eta-indicator">
            {eta > 0 ? `${formatTime(eta)} remaining` : 'Calculating...'}
          </span>
        </div>
      </div>
    </div>
  );
}
