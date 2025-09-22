'use client';

import { useState } from 'react';

interface FileDownloadProps {
  onDownload: (port: number, password?: string) => Promise<void>;
  isDownloading: boolean;
}

export default function FileDownload({ onDownload, isDownloading }: FileDownloadProps) {
  const [inviteCode, setInviteCode] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isFocused, setIsFocused] = useState(false);
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    
    const port = parseInt(inviteCode.trim(), 10);
    if (isNaN(port) || port <= 0 || port > 65535) {
      setError('Please enter a valid invite code (1-65535)');
      return;
    }
    
    try {
      await onDownload(port, password.trim() || undefined);
      setInviteCode(''); // Clear on success
      setPassword(''); // Clear password on success
    } catch (err) {
      setError('Failed to connect. Please verify the invite code and password.');
    }
  };
  
  return (
    <div className="space-y-6">
      {/* Info Card */}
      <div className="p-6 rounded-xl bg-gradient-to-br from-primary/5 to-primary/10 border border-primary/20">
        <div className="flex items-start space-x-3">
          <div className="p-2 bg-primary/10 rounded-lg">
            <svg 
              className="w-5 h-5 text-primary" 
              fill="none" 
              stroke="currentColor" 
              strokeWidth="2" 
              viewBox="0 0 24 24"
            >
              <path 
                strokeLinecap="round" 
                strokeLinejoin="round" 
                d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" 
              />
            </svg>
          </div>
          <div className="flex-1">
            <h3 className="text-base font-semibold text-foreground mb-1">
              Ready to receive
            </h3>
            <p className="text-sm text-muted-foreground">
              Enter the invite code shared by the sender to establish a secure connection.
            </p>
          </div>
        </div>
      </div>
      
      <form onSubmit={handleSubmit} className="space-y-5">
        <div className="space-y-2">
          <label 
            htmlFor="inviteCode" 
            className="block text-sm font-medium text-foreground"
          >
            Invite Code
          </label>
          <div className="relative">
            <input
              type="text"
              id="inviteCode"
              value={inviteCode}
              onChange={(e) => {
                setInviteCode(e.target.value);
                setError(''); // Clear error on input change
              }}
              onFocus={() => setIsFocused(true)}
              onBlur={() => setIsFocused(false)}
              placeholder="Enter 5-digit code"
              className={`
                input-field pr-12 font-mono text-lg tracking-wider
                ${error ? 'border-destructive focus:ring-destructive' : ''}
                ${isFocused && !error ? 'border-primary' : ''}
              `}
              disabled={isDownloading}
              required
              maxLength={5}
              pattern="[0-9]{1,5}"
            />
            <div className={`
              absolute right-3 top-1/2 -translate-y-1/2 transition-opacity duration-200
              ${inviteCode.length > 0 ? 'opacity-100' : 'opacity-0'}
            `}>
              <svg 
                className="w-5 h-5 text-muted-foreground" 
                fill="none" 
                stroke="currentColor" 
                strokeWidth="2" 
                viewBox="0 0 24 24"
              >
                <path 
                  strokeLinecap="round" 
                  strokeLinejoin="round" 
                  d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" 
                />
                <path 
                  strokeLinecap="round" 
                  strokeLinejoin="round" 
                  d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" 
                />
              </svg>
            </div>
          </div>
          
          {/* Error Message */}
          {error && (
            <div className="flex items-center space-x-2 text-destructive animate-in slide-in-from-top-1">
              <svg 
                className="w-4 h-4 flex-shrink-0" 
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
              <p className="text-sm">{error}</p>
            </div>
          )}
        </div>
        
        {/* Password input */}
        <div className="space-y-2">
          <label 
            htmlFor="password" 
            className="block text-sm font-medium text-foreground"
          >
            Password (if required)
          </label>
          <input
            type="password"
            id="password"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value);
              setError(''); // Clear error on input change
            }}
            placeholder="Enter password if file is protected"
            className="input-field"
            disabled={isDownloading}
          />
          <p className="text-xs text-muted-foreground">
            Only required if the sender set a password for this file
          </p>
        </div>
        
        <button
          type="submit"
          className="btn-primary w-full flex items-center justify-center space-x-2 py-3"
          disabled={isDownloading || !inviteCode.trim()}
        >
          {isDownloading ? (
            <>
              <div className="spinner h-4 w-4 border-2"></div>
              <span>Connecting...</span>
            </>
          ) : (
            <>
              <svg 
                className="w-5 h-5" 
                fill="none" 
                stroke="currentColor" 
                strokeWidth="2" 
                viewBox="0 0 24 24"
              >
                <path 
                  strokeLinecap="round" 
                  strokeLinejoin="round" 
                  d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 19l3 3m0 0l3-3m-3 3V10" 
                />
              </svg>
              <span>Start Download</span>
            </>
          )}
        </button>
      </form>
      
      {/* Help Text */}
      <div className="text-center">
        <p className="text-xs text-muted-foreground">
          Connection is encrypted end-to-end • No data stored on servers
        </p>
      </div>
    </div>
  );
}