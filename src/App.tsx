import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  MessageSquare, 
  Code, 
  Play, 
  Files, 
  Send, 
  Plus, 
  ChevronLeft,
  Terminal,
  Save,
  Trash2
} from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';

// Types
interface Message {
  role: 'user' | 'model';
  content: string;
}

interface ProjectFile {
  name: string;
  content: string;
  language: string;
}

export default function App() {
  const [view, setView] = useState<'chat' | 'editor' | 'preview'>('chat');
  const [messages, setMessages] = useState<Message[]>([
    { role: 'model', content: "Hello! I'm CodeBot. I can help you write code and preview it. Try asking me to 'Create a simple HTML contact form'." }
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [files, setFiles] = useState<ProjectFile[]>([
    { name: 'index.html', content: '<!DOCTYPE html>\n<html>\n<head>\n<style>\n  body { font-family: sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; background: #f8fafc; color: #1e293b; }\n  .card { background: white; padding: 2rem; border-radius: 1rem; shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1); border: 1px solid #e2e8f0; }\n</style>\n</head>\n<body>\n  <div class="card">\n    <h1>Welcome to CodeBot</h1>\n    <p>Ask the AI to generate some code to see it here!</p>\n  </div>\n</body>\n</html>', language: 'html' }
  ]);
  const [activeFileIndex, setActiveFileIndex] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');

  const chatEndRef = useRef<HTMLDivElement>(null);

  // Load from localStorage on mount
  useEffect(() => {
    const savedFiles = localStorage.getItem('codebot_files');
    if (savedFiles) {
      try {
        setFiles(JSON.parse(savedFiles));
      } catch (e) {
        console.error('Failed to parse saved files', e);
      }
    }
  }, []);

  // Auto-save to localStorage every 30 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      localStorage.setItem('codebot_files', JSON.stringify(files));
      console.log('Project auto-saved');
    }, 30000);

    return () => clearInterval(interval);
  }, [files]);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Auto-extraction logic: Extract code from model output
  const extractAndAutoSave = (text: string) => {
    const codeBlockRegex = /```(\w+)\n([\s\S]*?)```/g;
    let match;
    const newFiles = [...files];
    let updated = false;

    while ((match = codeBlockRegex.exec(text)) !== null) {
      const lang = match[1];
      const content = match[2];
      
      // Heuristic for naming
      let fileName = 'styles.css';
      if (lang === 'html' || content.includes('<!DOCTYPE html>')) fileName = 'index.html';
      else if (lang === 'javascript' || lang === 'js') fileName = 'script.js';
      else if (lang === 'typescript' || lang === 'ts') fileName = 'app.ts';
      else fileName = `code_${newFiles.length}.${lang}`;

      const existingIndex = newFiles.findIndex(f => f.name === fileName);
      if (existingIndex > -1) {
        newFiles[existingIndex] = { ...newFiles[existingIndex], content };
      } else {
        newFiles.push({ name: fileName, content, language: lang });
      }
      updated = true;
    }

    if (updated) {
      setFiles(newFiles);
      console.log('Automated: Files updated/created from AI response.');
    }
  };

  const handleSend = async () => {
    if (!input.trim() || loading) return;

    const userMessage = input;
    setMessages(prev => [...prev, { role: 'user', content: userMessage }]);
    setInput('');
    setLoading(true);

    try {
      const res = await fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: userMessage, history: messages.map(m => ({ role: m.role, parts: [{ text: m.content }] })) })
      });
      const data = await res.json();
      setMessages(prev => [...prev, { role: 'model', content: data.text }]);

      // Automation: Extract and save files without copy-paste
      extractAndAutoSave(data.text);

    } catch (err) {
      setMessages(prev => [...prev, { role: 'model', content: "Automation Error: Connection failed. Check your API setup." }]);
    } finally {
      setLoading(false);
    }
  };

  const activeFile = files[activeFileIndex];

  return (
    <div className="flex h-screen bg-[#0f172a] text-[#f1f5f9] font-sans overflow-hidden">
      {/* Sidebar - File Explorer */}
      <aside className="w-56 bg-[#020617] border-r border-[#334155] flex flex-col hidden md:flex">
        <div className="p-5 border-b border-[#334155] flex items-center gap-3">
          <div className="w-6 h-6 bg-[#38bdf8] rounded-md shadow-[0_0_10px_rgba(56,189,248,0.3)]"></div>
          <span className="font-bold text-sm tracking-widest text-[#38bdf8]">CODEBOT AI</span>
        </div>
        
        <nav className="flex-1 overflow-y-auto py-4">
          <div className="px-4 mb-4">
            <h3 className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-[0.15em] mb-2">Project Files</h3>
            
            {/* Search Bar */}
            <div className="mb-3">
              <input 
                type="text"
                placeholder="Search files..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full bg-[#1e293b] border border-[#334155] rounded px-2 py-1 text-[11px] text-white focus:outline-none focus:border-[#38bdf8] transition-colors"
              />
            </div>

            <div className="space-y-0.5">
              {files
                .filter(f => f.name.toLowerCase().includes(searchTerm.toLowerCase()))
                .map((f, i) => {
                  const originalIndex = files.indexOf(f);
                  return (
                    <button 
                      key={originalIndex}
                      onClick={() => {
                        setActiveFileIndex(originalIndex);
                        setView('editor');
                      }}
                      className={`w-full flex items-center gap-2.5 px-3 py-1.5 rounded text-[13px] transition-colors group ${originalIndex === activeFileIndex ? 'bg-[#38bdf8]/10 text-white border-r-2 border-[#38bdf8]' : 'text-[#94a3b8] hover:bg-white/5 hover:text-[#f1f5f9]'}`}
                    >
                      <Files className={`w-3.5 h-3.5 ${originalIndex === activeFileIndex ? 'text-[#38bdf8]' : 'text-[#94a3b8]'}`} />
                      <span className="truncate flex-1 text-left">{f.name}</span>
                    </button>
                  );
                })}
            </div>
            <button 
              onClick={() => {
                const name = prompt('File name:');
                if (name) setFiles(prev => [...prev, { name, content: '', language: name.split('.').pop() || 'text' }]);
              }}
              className="mt-3 flex items-center gap-2 px-3 py-1.5 rounded text-[11px] font-bold text-[#38bdf8] hover:bg-[#38bdf8]/10 transition-colors w-full uppercase tracking-wider"
            >
              <Plus className="w-3.5 h-3.5" />
              Add File
            </button>
          </div>

          <div className="px-4 mt-8 border-t border-[#334155] pt-4">
             <button 
                onClick={() => setView('chat')}
                className={`w-full flex items-center gap-2.5 px-3 py-1.5 rounded text-[13px] transition-colors ${view === 'chat' ? 'bg-[#38bdf8]/10 text-white' : 'text-[#94a3b8] hover:bg-white/5'}`}
             >
                <MessageSquare className="w-4 h-4" />
                Assistant
             </button>
          </div>
        </nav>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col min-w-0 bg-[#0f172a]">
        {/* Header/Breadcrumb */}
        <header className="h-12 border-b border-[#334155] bg-[#0f172a]/80 backdrop-blur flex items-center justify-between px-4 z-20">
          <div className="flex items-center gap-3">
             <button className="md:hidden text-[#94a3b8] p-1"><Terminal className="w-5 h-5"/></button>
             <span className="text-[11px] font-bold text-[#94a3b8] uppercase tracking-widest flex items-center gap-2">
                {view === 'chat' ? 'Conversation' : `Editing / ${activeFile?.name}`}
             </span>
          </div>

          <div className="flex items-center gap-2">
            <button 
              onClick={() => setView('chat')}
              className={`p-1.5 rounded-md transition-all ${view === 'chat' ? 'bg-[#38bdf8] text-slate-900' : 'text-[#94a3b8] hover:bg-white/5'}`}
              title="Chat"
            >
              <MessageSquare className="w-4 h-4" />
            </button>
            <button 
              onClick={() => setView('editor')}
              className={`p-1.5 rounded-md transition-all ${view === 'editor' ? 'bg-[#38bdf8] text-slate-900' : 'text-[#94a3b8] hover:bg-white/5'}`}
              title="Editor"
            >
              <Code className="w-4 h-4" />
            </button>
            <div className="w-px h-4 bg-[#334155] mx-1"></div>
            <button 
              onClick={() => setView('preview')}
              className={`flex items-center gap-2 px-3 py-1 rounded-md text-xs font-bold transition-all ${view === 'preview' ? 'bg-[#10b981] text-white' : 'bg-[#10b981]/10 text-[#10b981] hover:bg-[#10b981]/20'}`}
            >
              <Play className="w-3.5 h-3.5" />
              RUN
            </button>
          </div>
        </header>

        <div className="flex-1 relative overflow-hidden">
          <AnimatePresence mode="wait">
            {view === 'chat' && (
              <motion.div 
                key="chat"
                initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                className="h-full flex flex-col"
              >
                <div className="flex-1 overflow-y-auto p-6 space-y-6">
                  {messages.map((m, i) => (
                    <div key={i} className={`flex flex-col gap-2 ${m.role === 'user' ? 'items-end' : 'items-start'}`}>
                      <div className={`max-w-[90%] rounded-xl px-4 py-3 leading-relaxed text-[13.5px] ${
                        m.role === 'user' 
                          ? 'bg-[#38bdf8] text-slate-950 font-medium' 
                          : 'bg-[#1e293b] border border-[#334155] text-[#f1f5f9]'
                      }`}>
                        <div className="markdown-body prose prose-invert">
                          <ReactMarkdown 
                            components={{
                              code({node, inline, className, children, ...props}: any) {
                                const match = /language-(\w+)/.exec(className || '')
                                return !inline && match ? (
                                  <div className="relative group/code mt-3">
                                    <div className="flex items-center justify-between px-3 py-1.5 bg-[#121212] border-x border-t border-[#334155] rounded-t-lg">
                                      <span className="text-[10px] font-mono text-[#94a3b8] uppercase tracking-widest">{match[1]}</span>
                                      <button 
                                        onClick={() => {
                                          const fileName = prompt(`Found ${match[1]} code. Give it a file name:`, `snippet.${match[1] === 'javascript' ? 'js' : match[1] === 'html' ? 'html' : 'css'}`);
                                          if (fileName) {
                                            setFiles(prev => [...prev, { name: fileName, content: String(children), language: match[1] }]);
                                          }
                                        }}
                                        className="flex items-center gap-1.5 text-[#38bdf8] hover:text-white transition-colors text-[10px] font-bold uppercase tracking-wider"
                                      >
                                        <Save className="w-3 h-3" />
                                        Save to Files
                                      </button>
                                    </div>
                                    <SyntaxHighlighter
                                      style={vscDarkPlus}
                                      language={match[1]}
                                      PreTag="div"
                                      className="!bg-[#000000] !p-4 !rounded-b-lg !border-l-4 !border-[#38bdf8] !text-[12px] !font-mono !m-0 !mt-0 !border-t-0"
                                      {...props}
                                    >
                                      {String(children).replace(/\n$/, '')}
                                    </SyntaxHighlighter>
                                  </div>
                                ) : (
                                  <code className={`${className} bg-white/5 px-1.5 py-0.5 rounded text-[#38bdf8]`} {...props}>
                                    {children}
                                  </code>
                                )
                              }
                            }}
                          >
                            {m.content}
                          </ReactMarkdown>
                        </div>
                      </div>
                    </div>
                  ))}
                  {loading && (
                    <div className="flex items-center gap-3 text-[#94a3b8] text-[12px] animate-pulse">
                      <div className="w-2 h-2 rounded-full bg-[#38bdf8]"></div>
                      AI is formulating code...
                    </div>
                  )}
                  <div ref={chatEndRef} />
                </div>

                <div className="p-4 bg-[#020617]/50 border-t border-[#334155]">
                  <div className="flex items-center gap-3 max-w-3xl mx-auto">
                    <input 
                      value={input}
                      onChange={(e) => setInput(e.target.value)}
                      onKeyDown={(e) => e.key === 'Enter' && handleSend()}
                      placeholder="Request code or ask a question..."
                      className="flex-1 bg-[#1e293b] border border-[#334155] rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:border-[#38bdf8] transition-colors text-white"
                    />
                    <button 
                      onClick={handleSend}
                      disabled={loading || !input.trim()}
                      className="bg-[#38bdf8] text-slate-950 px-4 py-2.5 rounded-lg text-sm font-bold shadow-lg shadow-sky-500/10 hover:shadow-sky-500/20 active:scale-95 transition-all disabled:opacity-50"
                    >
                      SEND
                    </button>
                  </div>
                </div>
              </motion.div>
            )}

            {view === 'editor' && (
              <motion.div 
                key="editor"
                initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                className="h-full flex flex-col bg-[#000000]"
              >
                <div className="flex-1 flex flex-col min-h-0">
                  <div className="bg-[#1e293b] px-4 py-1.5 border-b border-[#334155] flex items-center justify-between">
                     <div className="flex items-center gap-2">
                        <Terminal className="w-3.5 h-3.5 text-[#38bdf8]" />
                        <span className="text-[12px] font-mono text-[#f1f5f9]">{activeFile?.name}</span>
                     </div>
                     <span className="text-[10px] text-[#94a3b8] font-mono uppercase tracking-widest">{activeFile?.language}</span>
                  </div>
                  <textarea 
                    value={activeFile?.content}
                    onChange={(e) => {
                      const newFiles = [...files];
                      newFiles[activeFileIndex].content = e.target.value;
                      setFiles(newFiles);
                    }}
                    className="flex-1 p-6 bg-transparent text-[#cbd5e1] font-mono text-[13px] resize-none focus:outline-none leading-relaxed border-none"
                    spellCheck={false}
                  />
                </div>
              </motion.div>
            )}

            {view === 'preview' && (
              <motion.div 
                key="preview"
                initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                className="h-full flex flex-col bg-[#111827]"
              >
                <div className="flex-1 p-4 flex flex-col min-h-0">
                   <div className="bg-[#e5e7eb] h-7 rounded-t-lg border-b border-slate-300 flex items-center px-4 gap-1.5">
                      <div className="w-2.5 h-2.5 rounded-full bg-[#9ca3af]"></div>
                      <div className="w-2.5 h-2.5 rounded-full bg-[#9ca3af]"></div>
                      <div className="w-2.5 h-2.5 rounded-full bg-[#9ca3af]"></div>
                      <div className="ml-3 bg-white h-4 flex-1 rounded text-[10px] flex items-center px-2 text-slate-400 font-mono truncate">
                        http://localhost:3000/{activeFile?.name}
                      </div>
                   </div>
                   <div className="flex-1 bg-white rounded-b-lg overflow-hidden shadow-2xl">
                    <iframe 
                      className="w-full h-full border-none"
                      srcDoc={activeFile?.content}
                      title="Preview"
                    />
                   </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </main>

      {/* Right Sidebar - Output Overlay / Preview Stats */}
      <aside className="w-72 bg-[#111827] border-l border-[#334155] flex flex-col hidden lg:flex">
         <div className="p-4 border-b border-[#334155] flex items-center justify-between bg-[#0f172a]">
            <span className="text-[11px] font-bold text-[#94a3b8] tracking-widest uppercase">Live Status</span>
            <div className="flex items-center gap-1.5">
               <div className="w-2 h-2 rounded-full bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)] animate-pulse"></div>
               <span className="text-[10px] text-emerald-500 font-bold">READY</span>
            </div>
         </div>
         
         <div className="flex-1 overflow-hidden flex flex-col">
            <div className="p-4 flex-1 overflow-y-auto">
               <div className="space-y-4">
                  <div className="bg-[#1e293b] p-3 rounded-lg border border-[#334155]">
                     <h4 className="text-[10px] font-bold text-[#38bdf8] uppercase tracking-wider mb-2">Active Context</h4>
                     <p className="text-[12px] text-[#f1f5f9] leading-relaxed">
                        Currently processing <strong>{files.length}</strong> files. CodeBot is monitoring your changes in real-time.
                     </p>
                  </div>

                  <div className="space-y-2">
                     <span className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider">Console Output</span>
                     <div className="bg-[#000000] p-3 rounded border border-[#334155] font-mono text-[10px] leading-relaxed text-[#94a3b8]">
                        <div className="text-emerald-500">[14:02:11] Index.html loaded successfully.</div>
                        <div>[14:02:12] Assets optimized. Rendering...</div>
                        <div className="text-amber-500">[14:02:15] Warning: Font non-standard.</div>
                        <div className="animate-pulse">_</div>
                     </div>
                  </div>
               </div>
            </div>

            <div className="p-4 border-t border-[#334155] bg-[#020617]/40">
               <button 
                  onClick={() => setView('preview')}
                  className="w-full bg-[#10b981] hover:bg-[#059669] text-white py-2.5 rounded-lg text-xs font-bold transition-all shadow-lg shadow-emerald-950/20 active:scale-95"
               >
                  RUN PROJECT
               </button>
            </div>
         </div>
      </aside>

      {/* Footer Mobile Nav */}
      <div className="md:hidden flex h-14 bg-[#020617] border-t border-[#334155] items-center justify-around px-4">
        {[
          { id: 'chat', icon: MessageSquare, label: 'Chat' },
          { id: 'editor', icon: Code, label: 'Code' },
          { id: 'preview', icon: Play, label: 'Run' }
        ].map((item) => (
          <button 
            key={item.id}
            onClick={() => setView(item.id as any)}
            className={`flex flex-col items-center gap-1 ${view === item.id ? 'text-[#38bdf8]' : 'text-[#94a3b8]'}`}
          >
            <item.icon className="w-5 h-5" />
            <span className="text-[10px] font-bold uppercase tracking-tighter">{item.label}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
