'use client';

import { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';

interface FileUploadProps {
  onFileUpload: (file: File, password?: string) => void;
  isUploading: boolean;
}

export default function FileUpload({ onFileUpload, isUploading }: FileUploadProps) {
  const [dragActive, setDragActive] = useState(false);
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  
  const onDrop = useCallback((acceptedFiles: File[]) => {
    if (acceptedFiles.length > 0) {
      onFileUpload(acceptedFiles[0], password.trim() || undefined);
    }
  }, [onFileUpload, password]);
  
  const { getRootProps, getInputProps } = useDropzone({ 
    onDrop,
    multiple: false,
    onDragEnter: () => setDragActive(true),
    onDragLeave: () => setDragActive(false),
    onDropAccepted: () => setDragActive(false),
    onDropRejected: () => setDragActive(false),
    disabled: isUploading,
  });

  return (
    <div 
      {...getRootProps()} 
      className={`
        relative w-full p-12 border-2 border-dashed rounded-xl text-center cursor-pointer
        transition-all duration-300 group
        ${dragActive 
          ? 'border-primary bg-primary/5 scale-[1.02]' 
          : 'border-border hover:border-primary/50 hover:bg-accent/50'
        }
        ${isUploading ? 'opacity-50 pointer-events-none' : ''}
      `}
    >
      <input {...getInputProps()} />
      
      {/* Animated background pattern */}
      <div className="absolute inset-0 opacity-5">
        <div className="absolute inset-0 bg-gradient-to-br from-primary/20 to-transparent rounded-xl" />
      </div>
      
      <div className="relative flex flex-col items-center justify-center space-y-4">
        <div className={`
          p-4 rounded-full transition-all duration-300
          ${dragActive ? 'bg-primary/20 scale-110' : 'bg-primary/10 group-hover:bg-primary/15'}
        `}>
          <svg 
            className={`w-8 h-8 transition-all duration-300 ${dragActive ? 'text-primary' : 'text-primary/70 group-hover:text-primary'}`}
            fill="none" 
            stroke="currentColor" 
            strokeWidth="2" 
            viewBox="0 0 24 24"
          >
            <path 
              strokeLinecap="round" 
              strokeLinejoin="round" 
              d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" 
            />
          </svg>
        </div>
        
        <div className="space-y-2">
          <p className="text-lg font-semibold text-foreground">
            Drop your file here, or <span className="text-primary">browse</span>
          </p>
          <p className="text-sm text-muted-foreground">
            Supports any file type • Max 2GB
          </p>
        </div>
        
        {/* Visual feedback for drag state */}
        {dragActive && (
          <div className="absolute inset-0 pointer-events-none">
            <div className="h-full w-full rounded-xl border-2 border-primary animate-pulse" />
          </div>
        )}
      </div>
      
      {/* Password input */}
      <div className="mt-6 space-y-2">
        <label className="text-sm font-medium text-foreground">
          Password Protection (Optional)
        </label>
        <div className="relative">
          <input
            type={showPassword ? 'text' : 'password'}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Enter password to protect this file"
            className="w-full px-3 py-2 border border-border rounded-lg bg-background text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary"
            disabled={isUploading}
          />
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="absolute right-3 top-1/2 transform -translate-y-1/2 text-muted-foreground hover:text-foreground"
            disabled={isUploading}
          >
            {showPassword ? '👁️' : '👁️‍🗨️'}
          </button>
        </div>
        <p className="text-xs text-muted-foreground">
          Recipients will need this password to download the file
        </p>
      </div>
    </div>
  );
}