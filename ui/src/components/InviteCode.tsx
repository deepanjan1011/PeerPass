'use client';

import { useState, useEffect } from 'react';

interface InviteCodeProps {
  port: number | null;
}

export default function InviteCode({ port }: InviteCodeProps) {
  const [copied, setCopied] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);
  
  useEffect(() => {
    if (port) {
      setShowSuccess(true);
    }
  }, [port]);
  
  if (!port) return null;
  
  const copyToClipboard = async () => {
    try {
      await navigator.clipboard.writeText(port.toString());
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    } catch (err) {
      // Fallback for older browsers
      const textArea = document.createElement('textarea');
      textArea.value = port.toString();
      document.body.appendChild(textArea);
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    }
  };
  
  return (
    <div className={`
      mt-6 p-6 rounded-xl border-2 transition-all duration-500
      ${showSuccess ? 'animate-in slide-in-from-bottom-3' : ''}
      bg-gradient-to-br from-green-500/5 via-emerald-500/5 to-teal-500/5
      border-green-500/20
    `}>
      {/* Success Header */}
      <div className="flex items-center space-x-3 mb-4">
        <div className="relative">
          <div className="absolute inset-0 bg-green-500 rounded-full animate-ping opacity-25"></div>
          <div className="relative p-2 bg-green-500/10 rounded-full">
            <svg 
              className="w-5 h-5 text-green-600" 
              fill="none" 
              stroke="currentColor" 
              strokeWidth="2" 
              viewBox="0 0 24 24"
            >
              <path 
                strokeLinecap="round" 
                strokeLinejoin="round" 
                d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" 
              />
            </svg>
          </div>
        </div>
        <div>
          <h3 className="text-lg font-semibold text-foreground">
            Ready to share!
          </h3>
          <p className="text-sm text-muted-foreground">
            Your file is available for transfer
          </p>
        </div>
      </div>
      
      {/* Invite Code Display */}
      <div className="space-y-3">
        <label className="block text-sm font-medium text-foreground">
          Share this invite code:
        </label>
        <div className="flex items-stretch gap-2">
          <div className="flex-1 relative group">
            <div className="absolute inset-0 bg-gradient-to-r from-primary/20 to-primary/10 rounded-lg blur-xl opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
            <div className="relative flex items-center justify-center bg-card border border-border rounded-lg px-6 py-4">
              <span className="font-mono text-2xl font-bold tracking-[0.25em] text-foreground select-all">
                {port.toString().padStart(5, '0')}
              </span>
            </div>
          </div>
          
          <button
            onClick={copyToClipboard}
            className={`
              px-5 rounded-lg font-medium transition-all duration-200
              flex items-center justify-center min-w-[120px]
              ${copied 
                ? 'bg-green-500 text-white hover:bg-green-600' 
                : 'bg-primary text-primary-foreground hover:bg-primary/90'
              }
              active:scale-95 shadow-sm hover:shadow-md
            `}
            aria-label="Copy invite code"
          >
            {copied ? (
              <>
                <svg 
                  className="w-5 h-5 mr-2" 
                  fill="none" 
                  stroke="currentColor" 
                  strokeWidth="2" 
                  viewBox="0 0 24 24"
                >
                  <path 
                    strokeLinecap="round" 
                    strokeLinejoin="round" 
                    d="M5 13l4 4L19 7" 
                  />
                </svg>
                Copied!
              </>
            ) : (
              <>
                <svg 
                  className="w-5 h-5 mr-2" 
                  fill="none" 
                  stroke="currentColor" 
                  strokeWidth="2" 
                  viewBox="0 0 24 24"
                >
                  <path 
                    strokeLinecap="round" 
                    strokeLinejoin="round" 
                    d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" 
                  />
                </svg>
                Copy
              </>
            )}
          </button>
        </div>
      </div>
      
      {/* Additional Info */}
      <div className="mt-4 flex items-start space-x-2">
        <svg 
          className="w-4 h-4 text-muted-foreground mt-0.5 flex-shrink-0" 
          fill="none" 
          stroke="currentColor" 
          strokeWidth="2" 
          viewBox="0 0 24 24"
        >
          <path 
            strokeLinecap="round" 
            strokeLinejoin="round" 
            d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" 
          />
        </svg>
        <div className="text-xs text-muted-foreground space-y-1">
          <p>Keep this tab open while sharing</p>
          <p>The connection will remain active until you close this page</p>
        </div>
      </div>
      
      {/* Visual Connection Status */}
      <div className="mt-4 pt-4 border-t border-border/50">
        <div className="flex items-center justify-between text-xs">
          <div className="flex items-center space-x-2">
            <div className="relative">
              <div className="absolute inset-0 bg-green-500 rounded-full animate-pulse"></div>
              <div className="relative w-2 h-2 bg-green-500 rounded-full"></div>
            </div>
            <span className="text-muted-foreground">Connection active</span>
          </div>
          <span className="text-muted-foreground font-mono">Port: {port}</span>
        </div>
      </div>
    </div>
  );
}