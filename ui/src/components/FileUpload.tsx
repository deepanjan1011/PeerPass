'use client';

import { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';

interface FileUploadProps {
  onFileUpload: (file: File) => void;
  isUploading: boolean;
}

export default function FileUpload({ onFileUpload, isUploading }: FileUploadProps) {
  const [dragActive, setDragActive] = useState(false);
  
  const onDrop = useCallback((acceptedFiles: File[]) => {
    if (acceptedFiles.length > 0) {
      onFileUpload(acceptedFiles[0]);
    }
  }, [onFileUpload]);
  
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
      
    </div>
  );
}