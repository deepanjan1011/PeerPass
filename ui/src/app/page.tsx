'use client';

import { useState, useRef } from 'react';
import FileUpload from '@/components/FileUpload';
import FileDownload from '@/components/FileDownload';
import InviteCode from '@/components/InviteCode';
import ProgressBar from '@/components/ProgressBar';
import axios from 'axios';

export default function Home() {
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isDownloading, setIsDownloading] = useState(false);
  const [port, setPort] = useState<number | null>(null);
  const [fileId, setFileId] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'upload' | 'download'>('upload');
  const [downloadPassword, setDownloadPassword] = useState('');
  
  // Progress tracking states
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadSpeed, setUploadSpeed] = useState(0);
  const [uploadEta, setUploadEta] = useState(0);
  const [downloadProgress, setDownloadProgress] = useState(0);
  const [downloadSpeed, setDownloadSpeed] = useState(0);
  const [downloadEta, setDownloadEta] = useState(0);
  const [downloadedBytes, setDownloadedBytes] = useState(0);
  
  // Refs for tracking progress
  const uploadStartTime = useRef<number>(0);
  const downloadStartTime = useRef<number>(0);
  const lastUploadBytes = useRef<number>(0);
  const lastDownloadBytes = useRef<number>(0);

  const handleFileUpload = async (file: File, password?: string) => {
    setUploadedFile(file);
    setIsUploading(true);
    setUploadProgress(0);
    setUploadSpeed(0);
    setUploadEta(0);
    uploadStartTime.current = Date.now();
    lastUploadBytes.current = 0;
    
    try {
      const formData = new FormData();
      formData.append('file', file);
      if (password) {
        formData.append('password', password);
      }
      
      const response = await axios.post('/api/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total) {
            const progress = (progressEvent.loaded / progressEvent.total) * 100;
            setUploadProgress(progress);
            
            // Calculate speed and ETA
            const currentTime = Date.now();
            const elapsedTime = (currentTime - uploadStartTime.current) / 1000; // seconds
            const bytesPerSecond = progressEvent.loaded / elapsedTime;
            const mbps = bytesPerSecond / (1024 * 1024);
            setUploadSpeed(mbps);
            
            if (mbps > 0) {
              const remainingBytes = progressEvent.total - progressEvent.loaded;
              const eta = remainingBytes / bytesPerSecond;
              setUploadEta(eta);
            }
          }
        },
      });
      
      setPort(response.data.port);
      setFileId(response.data.fileId);
      
      // Show success message with file ID
      if (response.data.hasPassword) {
        alert(`File uploaded successfully! File ID: ${response.data.fileId}\n\nThis file is password protected. Share the code and password with recipients.`);
      } else {
        alert(`File uploaded successfully! File ID: ${response.data.fileId}\n\nShare this code with recipients to download the file.`);
      }
    } catch (error) {
      console.error('Error uploading file:', error);
      alert('Failed to upload file. Please try again.');
    } finally {
      setIsUploading(false);
    }
  };
  
  const handleDownload = async (port: number, password?: string) => {
    setIsDownloading(true);
    setDownloadProgress(0);
    setDownloadSpeed(0);
    setDownloadEta(0);
    setDownloadedBytes(0);
    downloadStartTime.current = Date.now();

    try {
      // Simple download - get the full file at once
      const url = password 
        ? `/api/download/${port}?password=${encodeURIComponent(password)}`
        : `/api/download/${port}`;
        
      const response = await axios.get(url, {
        responseType: 'arraybuffer',
        onDownloadProgress: (progressEvent) => {
          if (progressEvent.total) {
            const downloaded = progressEvent.loaded;
            const total = progressEvent.total;
            const progress = (downloaded / total) * 100;
            const elapsed = (Date.now() - downloadStartTime.current) / 1000;
            const speed = downloaded / elapsed;
            
            setDownloadedBytes(downloaded);
            setDownloadProgress(progress);
            setDownloadSpeed(speed / (1024 * 1024)); // MB/s
            setDownloadEta((total - downloaded) / speed);
          }
        }
      });

      // Get filename from Content-Disposition header
      let filename = 'downloaded-file';
      const contentDisposition = response.headers['content-disposition'];
      if (contentDisposition) {
        const match = /filename="(.+)"/i.exec(contentDisposition);
        if (match && match[1]) {
          filename = match[1];
        }
      }

      // Create and download the file
      const blob = new Blob([response.data]);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      
      // Add attributes to make the download appear safer
      link.setAttribute('type', 'application/octet-stream');
      link.setAttribute('rel', 'noopener noreferrer');
      
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      
      // Show success message
      console.log('File downloaded successfully:', filename);
      
    } catch (error) {
      console.error('Download failed:', error);
      alert('Download failed: ' + (error as Error).message);
    } finally {
      setIsDownloading(false);
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B';
    else if (bytes < 1024 * 1024) return Math.round(bytes / 1024) + ' KB';
    else if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    else return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB';
  };

  return (
    <div className="min-h-screen flex flex-col">
      {/* Navigation Bar */}
      <nav className="border-b border-border/40 backdrop-blur-md bg-background/95 sticky top-0 z-50">
        <div className="container mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="relative logo-container">
                <div className="w-10 h-10 bg-gradient-to-br from-primary to-primary/80 rounded-xl flex items-center justify-center shadow-lg shadow-primary/25">
                  <svg
                    className="w-6 h-6 text-white logo-icon"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2.5"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      d="M13 10V3L4 14h7v7l9-11h-7z"
                    />
                  </svg>
                </div>
                <div className="absolute -inset-1 bg-gradient-to-br from-primary/20 to-primary/10 rounded-xl blur-sm -z-10 logo-glow"></div>
              </div>
              <div className="flex flex-col">
                <h1 className="text-2xl font-bold bg-gradient-to-r from-primary via-primary/90 to-primary/70 bg-clip-text text-transparent">
                  PeerPass
                </h1>
              </div>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-sm text-muted-foreground hidden sm:inline">
                Secure P2P Transfer
              </span>
            </div>
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <div className="flex-1 container mx-auto px-4 py-12 max-w-5xl">
        <div className="text-center mb-12">
          <h2 className="text-4xl font-bold mb-4 bg-gradient-to-r from-primary to-primary/60 bg-clip-text text-transparent">
            File Sharing Platform
          </h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            Transfer files securely through peer-to-peer connections. No cloud storage, no intermediaries.
          </p>
        </div>
        
        <div className="card glass p-8 max-w-3xl mx-auto">
          {/* Tab Navigation */}
          <div className="flex border-b border-border mb-8">
            <button
              className={`tab-button ${
                activeTab === 'upload' ? 'tab-button-active' : 'tab-button-inactive'
              }`}
              onClick={() => setActiveTab('upload')}
            >
              <div className="flex items-center space-x-2">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                </svg>
                <span>Send File</span>
              </div>
            </button>
            <button
              className={`tab-button ${
                activeTab === 'download' ? 'tab-button-active' : 'tab-button-inactive'
              }`}
              onClick={() => setActiveTab('download')}
            >
              <div className="flex items-center space-x-2">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 19l3 3m0 0l3-3m-3 3V10" />
                </svg>
                <span>Receive File</span>
              </div>
            </button>
          </div>
          
          {activeTab === 'upload' ? (
            <div className="space-y-6">
              <FileUpload onFileUpload={handleFileUpload} isUploading={isUploading} />
              
              {uploadedFile && !isUploading && (
                <div className="p-4 bg-muted/50 rounded-lg border border-border">
                  <div className="flex items-center space-x-3">
                    <svg className="w-5 h-5 text-primary flex-shrink-0" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-foreground truncate">
                        {uploadedFile.name}
                      </p>
                      <p className="text-sm text-muted-foreground">
                        {formatFileSize(uploadedFile.size)}
                      </p>
                    </div>
                  </div>
                </div>
              )}
              
              {isUploading && uploadedFile && (
                <div className="p-6 bg-muted/50 rounded-lg border border-border">
                  <ProgressBar
                    progress={uploadProgress}
                    speed={uploadSpeed}
                    eta={uploadEta}
                    fileName={uploadedFile.name}
                    fileSize={uploadedFile.size}
                    transferred={uploadedFile.size * (uploadProgress / 100)}
                  />
                </div>
              )}
              
              <InviteCode port={port} />
            </div>
          ) : (
            <div className="space-y-6">
              <FileDownload onDownload={handleDownload} isDownloading={isDownloading} />
              
              {isDownloading && (
                <div className="p-6 bg-muted/50 rounded-lg border border-border">
                  <ProgressBar
                    progress={downloadProgress}
                    speed={downloadSpeed}
                    eta={downloadEta}
                    fileName="Downloading file..."
                    fileSize={downloadedBytes > 0 ? downloadedBytes / (downloadProgress / 100) : 0}
                    transferred={downloadedBytes}
                  />
                </div>
              )}
            </div>
          )}
        </div>

        {/* Features Section */}
        <div className="mt-16 grid md:grid-cols-3 gap-6 max-w-4xl mx-auto">
          <div className="text-center p-6">
            <div className="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center mx-auto mb-4">
              <svg className="w-6 h-6 text-primary" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
            </div>
            <h3 className="font-semibold mb-2">Real-time Progress</h3>
            <p className="text-sm text-muted-foreground">Track upload/download progress with speed and ETA indicators</p>
          </div>
          
          <div className="text-center p-6">
            <div className="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center mx-auto mb-4">
              <svg className="w-6 h-6 text-primary" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <h3 className="font-semibold mb-2">Direct Transfer</h3>
            <p className="text-sm text-muted-foreground">P2P connection ensures fast, direct file sharing</p>
          </div>
          
          <div className="text-center p-6">
            <div className="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center mx-auto mb-4">
              <svg className="w-6 h-6 text-primary" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
            </div>
            <h3 className="font-semibold mb-2">No Cloud Storage</h3>
            <p className="text-sm text-muted-foreground">Files never touch third-party servers</p>
          </div>
        </div>
      </div>
      
      {/* Footer */}
      <footer className="border-t border-border/40 mt-auto">
        <div className="container mx-auto px-4 py-6">
          <div className="flex flex-col sm:flex-row items-center justify-between text-sm text-muted-foreground">
            <p>© {new Date().getFullYear()} PeerPass. File Sharing Platform.</p>
            <div className="flex items-center space-x-4 mt-2 sm:mt-0">
              <a href="#" className="hover:text-foreground transition-colors">Privacy</a>
              <a href="#" className="hover:text-foreground transition-colors">Terms</a>
              <a href="#" className="hover:text-foreground transition-colors">Support</a>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}