import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  ArrowLeft,
  Bluetooth,
  ChevronDown,
  ChevronUp,
  Compass,
  Info,
  Menu,
  MessageSquare,
  Paperclip,
  Plus,
  RefreshCw,
  Reply,
  Send,
  Settings,
  Share2,
  SlidersHorizontal,
  Wifi,
  X,
} from 'lucide-react';
import {
  BannerNotification,
  ChatMessage,
  ChatThread,
  ConnectionMode,
  DiscoveredDevice,
  TabId,
  ThemeMode,
} from './types';
import {
  INITIAL_THREADS,
  MOCK_BT_DEVICES,
  MOCK_WIFI_DEVICES,
} from './data/initialData';

const STORAGE_KEYS = {
  username: 'tchat_username',
  signedIn: 'tchat_google_signed_in',
  theme: 'tchat_theme_mode',
  threads: 'tchat_threads',
};

function formatNowTime(): string {
  const now = new Date();
  return now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

export default function App() {
  // Navigation & UI state
  const [activeTab, setActiveTab] = useState<TabId>('home');
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [modeMenuOpen, setModeMenuOpen] = useState(false);
  const [newChatModalOpen, setNewChatModalOpen] = useState(false);

  // Theme state
  const [themeMode, setThemeMode] = useState<ThemeMode>(() => {
    const saved = localStorage.getItem(STORAGE_KEYS.theme);
    return saved === 'light' || saved === 'dark' || saved === 'system'
      ? saved
      : 'dark';
  });
  const [systemPrefersDark, setSystemPrefersDark] = useState<boolean>(() =>
    window.matchMedia('(prefers-color-scheme: dark)').matches
  );

  // Account & profile state
  const [username, setUsername] = useState<string>(
    () => localStorage.getItem(STORAGE_KEYS.username) || 'User'
  );
  const [isGoogleSignedIn, setIsGoogleSignedIn] = useState<boolean>(
    () => localStorage.getItem(STORAGE_KEYS.signedIn) === 'true'
  );
  const [isEditingUsername, setIsEditingUsername] = useState(false);
  const [usernameDraft, setUsernameDraft] = useState(username);

  // Chats & messages state
  const [threads, setThreads] = useState<ChatThread[]>(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEYS.threads);
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed;
      }
    } catch {
      // Fallback to defaults
    }
    return INITIAL_THREADS;
  });
  const [activeThreadId, setActiveThreadId] = useState<string>(
    INITIAL_THREADS[0].id
  );
  const [replyingTo, setReplyingTo] = useState<ChatMessage | null>(null);
  const [messageInput, setMessageInput] = useState('');
  const [pendingAttachment, setPendingAttachment] = useState<{
    name: string;
    sizeLabel: string;
  } | null>(null);

  // New chat creation modal fields
  const [newChatTitle, setNewChatTitle] = useState('');
  const [newChatMode, setNewChatMode] = useState<ConnectionMode>('Space');

  // Connections tab state
  const [myIp, setMyIp] = useState('192.168.0.100');
  const [connectionStatus, setConnectionStatus] = useState('Disconnected');
  const [wifiExpanded, setWifiExpanded] = useState(true);
  const [btExpanded, setBtExpanded] = useState(true);
  const [wifiScanning, setWifiScanning] = useState(false);
  const [discoveredWifi, setDiscoveredWifi] = useState<DiscoveredDevice[]>([]);
  const [btHostActive, setBtHostActive] = useState(false);
  const [btScanning, setBtScanning] = useState(false);
  const [discoveredBt, setDiscoveredBt] = useState<DiscoveredDevice[]>([]);
  const [targetIp, setTargetIp] = useState('');
  const [tcpHosting, setTcpHosting] = useState(false);

  // In-app sliding notification banner
  const [banner, setBanner] = useState<BannerNotification | null>(null);
  const bannerTimerRef = useRef<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);

  const activeThread = useMemo(
    () => threads.find((t) => t.id === activeThreadId) || threads[0],
    [threads, activeThreadId]
  );

  const isDarkTheme =
    themeMode === 'dark' || (themeMode === 'system' && systemPrefersDark);

  useEffect(() => {
    const media = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = (e: MediaQueryListEvent) => setSystemPrefersDark(e.matches);
    media.addEventListener('change', onChange);
    return () => media.removeEventListener('change', onChange);
  }, []);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.theme, themeMode);
  }, [themeMode]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.threads, JSON.stringify(threads));
  }, [threads]);

  useEffect(() => {
    if (activeTab === 'chat') {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [activeTab, activeThread?.messages.length]);

  const showBanner = (sender: string, message: string) => {
    if (bannerTimerRef.current) {
      window.clearTimeout(bannerTimerRef.current);
    }
    setBanner({ id: Date.now(), sender, message });
    bannerTimerRef.current = window.setTimeout(() => {
      setBanner(null);
    }, 3500);
  };

  const openThread = (threadId: string) => {
    setThreads((prev) =>
      prev.map((t) => (t.id === threadId ? { ...t, unreadCount: 0 } : t))
    );
    setActiveThreadId(threadId);
    setReplyingTo(null);
    setModeMenuOpen(false);
    setActiveTab('chat');
  };

  const switchTab = (tab: TabId) => {
    setActiveTab(tab);
    setDrawerOpen(false);
    setModeMenuOpen(false);
  };

  const handleSendMessage = () => {
    const trimmed = messageInput.trim();
    if (!trimmed && !pendingAttachment) return;
    if (!activeThread) return;

    const newMsg: ChatMessage = {
      id: `msg-${Date.now()}`,
      sender: username,
      text: trimmed || (pendingAttachment ? `Shared ${pendingAttachment.name}` : ''),
      timestamp: formatNowTime(),
      isOwn: true,
      replyTo: replyingTo
        ? {
            id: replyingTo.id,
            sender: replyingTo.sender,
            text: replyingTo.text,
          }
        : undefined,
      attachment: pendingAttachment || undefined,
    };

    const currentThreadId = activeThread.id;
    const currentThreadTitle = activeThread.title;
    const currentMode = activeThread.mode;

    setThreads((prev) =>
      prev.map((t) =>
        t.id === currentThreadId
          ? { ...t, messages: [...t.messages, newMsg] }
          : t
      )
    );
    setMessageInput('');
    setReplyingTo(null);
    setPendingAttachment(null);

    // Simulated peer acknowledgement for interactive local/space demo
    window.setTimeout(() => {
      const peerName =
        currentMode === 'Space' ? 'Alex' : currentThreadTitle.split(' ')[0];
      const replyMsg: ChatMessage = {
        id: `msg-${Date.now()}-reply`,
        sender: peerName,
        text: `Received via ${currentMode}: "${newMsg.text.slice(0, 48)}${
          newMsg.text.length > 48 ? '...' : ''
        }"`,
        timestamp: formatNowTime(),
        isOwn: false,
      };
      setThreads((prev) =>
        prev.map((t) =>
          t.id === currentThreadId
            ? {
                ...t,
                unreadCount: activeTab === 'chat' && activeThreadId === currentThreadId ? 0 : t.unreadCount + 1,
                messages: [...t.messages, replyMsg],
              }
            : t
        )
      );
      if (activeTab !== 'chat') {
        showBanner(peerName, replyMsg.text);
      }
    }, 900);
  };

  const handleCreateChat = (e: React.FormEvent) => {
    e.preventDefault();
    const title = newChatTitle.trim();
    if (!title) return;

    const formattedTitle =
      newChatMode === 'Space' && !title.startsWith('#') ? `#${title}` : title;
    const created: ChatThread = {
      id: `thread-${Date.now()}`,
      title: formattedTitle,
      mode: newChatMode,
      endpoint:
        newChatMode === 'Space'
          ? 'Local Mesh Space'
          : newChatMode === 'Bluetooth'
            ? 'BT Peer Link'
            : '192.168.0.x',
      unreadCount: 0,
      messages: [
        {
          id: `welcome-${Date.now()}`,
          sender: 'TChat',
          text: `${formattedTitle} created (${newChatMode} mode).`,
          timestamp: formatNowTime(),
          isOwn: false,
        },
      ],
    };

    setThreads((prev) => [created, ...prev]);
    setNewChatTitle('');
    setNewChatModalOpen(false);
    openThread(created.id);
    showBanner('TChat', `Created ${formattedTitle}`);
  };

  const handleRefreshIp = () => {
    const octet = 100 + Math.floor(Math.random() * 140);
    const nextIp = `192.168.0.${octet}`;
    setMyIp(nextIp);
    showBanner('Network Info', `Local IP refreshed: ${nextIp}`);
  };

  const handleScanWifi = () => {
    setWifiScanning(true);
    setWifiExpanded(true);
    window.setTimeout(() => {
      setDiscoveredWifi(MOCK_WIFI_DEVICES);
      setWifiScanning(false);
      showBanner(
        'Local Network',
        `Found ${MOCK_WIFI_DEVICES.length} devices on subnet`
      );
    }, 650);
  };

  const handleScanBt = () => {
    setBtScanning(true);
    setBtExpanded(true);
    window.setTimeout(() => {
      setDiscoveredBt(MOCK_BT_DEVICES);
      setBtScanning(false);
      showBanner(
        'Bluetooth',
        `Discovered ${MOCK_BT_DEVICES.length} nearby devices`
      );
    }, 650);
  };

  const handleConnectPeer = (device: DiscoveredDevice) => {
    const mode: ConnectionMode =
      device.kind === 'wifi' ? 'Local Wi-Fi' : 'Bluetooth';
    if (device.kind === 'wifi') {
      setDiscoveredWifi((prev) =>
        prev.map((d) => ({ ...d, connected: d.id === device.id }))
      );
    } else {
      setDiscoveredBt((prev) =>
        prev.map((d) => ({ ...d, connected: d.id === device.id }))
      );
    }
    setConnectionStatus(`Connected to ${device.name} (${mode})`);
    showBanner(device.name, `Connected via ${mode}`);

    const existing = threads.find((t) => t.endpoint === device.address);
    if (existing) {
      openThread(existing.id);
      return;
    }

    const peerThread: ChatThread = {
      id: `peer-${device.id}-${Date.now()}`,
      title: device.name,
      mode,
      endpoint: device.address,
      unreadCount: 0,
      messages: [
        {
          id: `msg-conn-${Date.now()}`,
          sender: device.name,
          text: `Peer session established over ${mode} (${device.address}).`,
          timestamp: formatNowTime(),
          isOwn: false,
        },
      ],
    };
    setThreads((prev) => [peerThread, ...prev]);
    openThread(peerThread.id);
  };

  const handleToggleBtHost = () => {
    const next = !btHostActive;
    setBtHostActive(next);
    setConnectionStatus(
      next ? 'Hosting Bluetooth RFCOMM server' : 'Disconnected'
    );
    showBanner(
      'Bluetooth Host',
      next ? 'BT Host started and discoverable' : 'BT Host stopped'
    );
  };

  const handleHostTcp = () => {
    const next = !tcpHosting;
    setTcpHosting(next);
    setConnectionStatus(
      next ? `Hosting TCP on ${myIp}:8888` : 'Disconnected'
    );
    showBanner(
      'TCP Socket',
      next ? `Listening on ${myIp}:8888` : 'TCP Host stopped'
    );
  };

  const handleConnectIp = () => {
    const ip = targetIp.trim();
    if (!ip) {
      showBanner('TCP Connection', 'Enter a Target Host IP Address first');
      return;
    }
    setConnectionStatus(`Connected to TCP ${ip}`);
    showBanner('TCP Connected', `Established socket link to ${ip}`);

    const existing = threads.find((t) => t.endpoint === ip);
    if (existing) {
      openThread(existing.id);
      return;
    }

    const tcpThread: ChatThread = {
      id: `tcp-${Date.now()}`,
      title: `TCP ${ip}`,
      mode: 'TCP IP',
      endpoint: ip,
      unreadCount: 0,
      messages: [
        {
          id: `tcp-init-${Date.now()}`,
          sender: `Peer (${ip})`,
          text: `Connected to host ${ip}.`,
          timestamp: formatNowTime(),
          isOwn: false,
        },
      ],
    };
    setThreads((prev) => [tcpThread, ...prev]);
    openThread(tcpThread.id);
  };

  const handleGoogleSignIn = () => {
    if (isGoogleSignedIn) {
      setIsGoogleSignedIn(false);
      localStorage.setItem(STORAGE_KEYS.signedIn, 'false');
      showBanner('Account', 'Disconnected Google account');
    } else {
      const nextName = username === 'User' ? 'Turizmo User' : username;
      setIsGoogleSignedIn(true);
      setUsername(nextName);
      setUsernameDraft(nextName);
      localStorage.setItem(STORAGE_KEYS.signedIn, 'true');
      localStorage.setItem(STORAGE_KEYS.username, nextName);
      showBanner('Google Sign-In', `Signed in as ${nextName}`);
    }
  };

  const handleSaveUsername = () => {
    const clean = usernameDraft.trim();
    if (!clean) return;
    setUsername(clean);
    setIsEditingUsername(false);
    localStorage.setItem(STORAGE_KEYS.username, clean);
    showBanner('Profile', `Username updated to ${clean}`);
  };

  const handleSaveProfile = () => {
    localStorage.setItem(STORAGE_KEYS.username, username);
    localStorage.setItem(STORAGE_KEYS.signedIn, String(isGoogleSignedIn));
    showBanner('Account & Profile', 'Profile preferences saved');
  };

  const handleSignOut = () => {
    setIsGoogleSignedIn(false);
    setUsername('User');
    setUsernameDraft('User');
    setConnectionStatus('Disconnected');
    localStorage.setItem(STORAGE_KEYS.signedIn, 'false');
    localStorage.setItem(STORAGE_KEYS.username, 'User');
    setDrawerOpen(false);
    showBanner('Signed Out', 'Reset active session to Guest User');
  };

  const handleSwitchThreadMode = (mode: ConnectionMode) => {
    if (!activeThread) return;
    setThreads((prev) =>
      prev.map((t) => (t.id === activeThread.id ? { ...t, mode } : t))
    );
    setModeMenuOpen(false);
    showBanner('Mode Selector', `${activeThread.title} switched to ${mode}`);
  };

  const handleAttachmentChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const kb = Math.max(1, Math.round(file.size / 1024));
    setPendingAttachment({
      name: file.name,
      sizeLabel: kb >= 1024 ? `${(kb / 1024).toFixed(1)} MB` : `${kb} KB`,
    });
    e.target.value = '';
  };

  return (
    <div
      className={`relative flex h-full w-full flex-col overflow-hidden ${
        isDarkTheme ? 'theme-dark' : ''
      }`}
      style={{
        backgroundColor: 'var(--surface)',
        color: 'var(--text-primary)',
      }}
    >
      {/* APP HEADER BAR (56dp) */}
      <header
        className="relative z-20 flex h-14 shrink-0 items-center px-3 shadow-sm"
        style={{
          backgroundColor: 'var(--primary-container)',
          color: 'var(--on-primary-container)',
        }}
      >
        {activeTab === 'chat' ? (
          <button
            type="button"
            onClick={() => switchTab('home')}
            className="mr-1 rounded-full p-1.5 transition hover:bg-black/10"
            aria-label="Back to Spaces"
          >
            <ArrowLeft className="h-6 w-6" />
          </button>
        ) : (
          <button
            type="button"
            onClick={() => setDrawerOpen(true)}
            className="rounded-full p-1 transition hover:bg-black/10"
            aria-label="Open navigation drawer"
          >
            <Menu className="h-7 w-7" />
          </button>
        )}

        <div className="ml-3 flex flex-1 flex-col">
          <span className="text-[20px] font-bold leading-tight">
            {activeTab === 'chat' && activeThread ? activeThread.title : 'TChat'}
          </span>
          {activeTab === 'chat' && activeThread && (
            <span className="text-[11px] opacity-75">
              {activeThread.mode}
              {activeThread.endpoint ? ` • ${activeThread.endpoint}` : ''}
            </span>
          )}
        </div>

        {/* Header Mode Swap Icon (visible in chat view as in activity_main.xml) */}
        {activeTab === 'chat' && (
          <div className="relative">
            <button
              type="button"
              onClick={() => setModeMenuOpen((v) => !v)}
              className="rounded-full p-1.5 transition hover:bg-black/10"
              aria-label="Mode Selector"
              title="Mode Selector"
            >
              <SlidersHorizontal className="h-6 w-6" />
            </button>

            {modeMenuOpen && (
              <div
                className="absolute right-0 top-10 z-40 w-44 rounded-xl border p-1.5 shadow-xl"
                style={{
                  backgroundColor: 'var(--surface-variant)',
                  borderColor: 'var(--border-subtle)',
                  color: 'var(--text-primary)',
                }}
              >
                <div
                  className="px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide"
                  style={{ color: 'var(--text-muted)' }}
                >
                  Connection Mode
                </div>
                {(
                  ['Space', 'Local Wi-Fi', 'Bluetooth', 'TCP IP'] as ConnectionMode[]
                ).map((mode) => (
                  <button
                    key={mode}
                    type="button"
                    onClick={() => handleSwitchThreadMode(mode)}
                    className="flex w-full items-center justify-between rounded-lg px-2.5 py-2 text-left text-sm transition hover:bg-[#8A2BE2]/15"
                  >
                    <span>{mode}</span>
                    {activeThread?.mode === mode && (
                      <span className="h-2 w-2 rounded-full bg-[#8A2BE2]" />
                    )}
                  </button>
                ))}
              </div>
            )}
          </div>
        )}
      </header>

      {/* IN-APP SLIDING NOTIFICATION BANNER OVERLAY */}
      {banner && (
        <div className="pointer-events-none absolute left-0 right-0 top-14 z-30 px-3 pt-3">
          <div
            onClick={() => setBanner(null)}
            className="pointer-events-auto mx-auto flex max-w-2xl cursor-pointer items-center rounded-2xl p-3 shadow-lg"
            style={{
              backgroundColor: 'var(--surface-variant)',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#8A2BE2]/15 text-[#8A2BE2]">
              <Info className="h-5 w-5" />
            </div>
            <div className="ml-3 min-w-0 flex-1">
              <div className="text-sm font-bold">{banner.sender}</div>
              <div
                className="truncate text-xs"
                style={{ color: 'var(--text-secondary)' }}
              >
                {banner.message}
              </div>
            </div>
            <X
              className="ml-2 h-4 w-4 shrink-0"
              style={{ color: 'var(--text-muted)' }}
            />
          </div>
        </div>
      )}

      {/* MAIN CONTAINER FOR TABS */}
      <main className="relative flex-1 overflow-hidden">
        {/* TAB 1: HOME */}
        {activeTab === 'home' && (
          <section className="relative flex h-full flex-col">
            <div className="px-4 py-2">
              <h2 className="text-sm font-bold text-[#8A2BE2]">
                Spaces &amp; Direct Messages
              </h2>
            </div>

            <div className="flex-1 overflow-y-auto px-3 pb-24">
              <div className="mx-auto flex max-w-3xl flex-col gap-2.5">
                {threads.map((thread) => {
                  const lastMsg = thread.messages[thread.messages.length - 1];
                  return (
                    <button
                      key={thread.id}
                      type="button"
                      onClick={() => openThread(thread.id)}
                      className="flex w-full items-center rounded-2xl p-3.5 text-left shadow-sm transition hover:brightness-105"
                      style={{
                        backgroundColor: 'var(--surface-variant)',
                        border: '1px solid var(--border-subtle)',
                      }}
                    >
                      <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[#8A2BE2]/20 text-[#8A2BE2]">
                        {thread.mode === 'Bluetooth' ? (
                          <Bluetooth className="h-5 w-5" />
                        ) : thread.mode === 'Local Wi-Fi' ||
                          thread.mode === 'TCP IP' ? (
                          <Wifi className="h-5 w-5" />
                        ) : (
                          <MessageSquare className="h-5 w-5" />
                        )}
                      </div>

                      <div className="ml-3 min-w-0 flex-1">
                        <div className="flex items-center justify-between gap-2">
                          <span className="truncate text-[15px] font-bold">
                            {thread.title}
                          </span>
                          <span
                            className="shrink-0 text-[11px]"
                            style={{ color: 'var(--text-muted)' }}
                          >
                            {lastMsg?.timestamp || ''}
                          </span>
                        </div>
                        <div className="mt-0.5 flex items-center justify-between gap-2">
                          <span
                            className="truncate text-xs"
                            style={{ color: 'var(--text-secondary)' }}
                          >
                            {lastMsg
                              ? `${lastMsg.sender}: ${lastMsg.text}`
                              : thread.mode}
                          </span>
                          <div className="flex shrink-0 items-center gap-1.5">
                            <span className="rounded-md bg-[#4A90E2]/15 px-1.5 py-0.5 text-[10px] font-semibold text-[#4A90E2]">
                              {thread.mode}
                            </span>
                            {thread.unreadCount > 0 && (
                              <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-[#8A2BE2] px-1.5 text-[11px] font-bold text-white">
                                {thread.unreadCount}
                              </span>
                            )}
                          </div>
                        </div>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* FLOATING ACTION BUTTON (fabNewChat) */}
            <button
              type="button"
              onClick={() => setNewChatModalOpen(true)}
              aria-label="New Chat or Space"
              title="New Chat or Space"
              className="absolute bottom-20 right-5 z-10 flex h-14 w-14 items-center justify-center rounded-2xl bg-[#8A2BE2] text-white shadow-lg transition hover:brightness-110 active:scale-95"
            >
              <Plus className="h-6 w-6" />
            </button>
          </section>
        )}

        {/* TAB 2: CONNECTIONS */}
        {activeTab === 'connections' && (
          <section className="h-full overflow-y-auto p-3 pb-24">
            <div className="mx-auto flex max-w-3xl flex-col gap-3">
              {/* IP / Router Info Card */}
              <div
                className="rounded-2xl p-4 shadow-sm"
                style={{
                  backgroundColor: 'var(--surface-variant)',
                  border: '1px solid var(--border-subtle)',
                }}
              >
                <div className="flex items-center justify-between">
                  <span
                    className="text-sm"
                    style={{ color: 'var(--text-secondary)' }}
                  >
                    Status: {connectionStatus}
                  </span>
                  <button
                    type="button"
                    onClick={handleRefreshIp}
                    aria-label="Refresh IP"
                    className="rounded-full p-1 text-[#8A2BE2] transition hover:bg-[#8A2BE2]/15"
                  >
                    <RefreshCw className="h-5 w-5" />
                  </button>
                </div>
                <div className="mt-1.5 text-[15px] font-bold text-[#4A90E2]">
                  Your IP: {myIp}
                </div>
              </div>

              {/* Local Router Devices Card */}
              <div
                className="rounded-2xl p-4 shadow-sm"
                style={{
                  backgroundColor: 'var(--surface-variant)',
                  border: '1px solid var(--border-subtle)',
                }}
              >
                <button
                  type="button"
                  onClick={() => setWifiExpanded((v) => !v)}
                  className="flex w-full items-center justify-between text-left"
                >
                  <span className="text-[15px] font-bold">
                    Local Network Devices
                  </span>
                  {wifiExpanded ? (
                    <ChevronUp className="h-5 w-5" />
                  ) : (
                    <ChevronDown className="h-5 w-5" />
                  )}
                </button>

                {wifiExpanded && (
                  <div className="mt-3">
                    <button
                      type="button"
                      onClick={handleScanWifi}
                      disabled={wifiScanning}
                      className="w-full rounded-xl bg-[#8A2BE2] px-4 py-2.5 text-sm font-semibold text-white transition hover:brightness-110 disabled:opacity-60"
                    >
                      {wifiScanning
                        ? 'Scanning Local Network...'
                        : 'Scan Local Network'}
                    </button>

                    {discoveredWifi.length > 0 && (
                      <div className="mt-3 flex flex-col gap-2">
                        {discoveredWifi.map((dev) => (
                          <div
                            key={dev.id}
                            className="flex items-center justify-between rounded-xl p-2.5"
                            style={{
                              backgroundColor: 'var(--surface)',
                              border: '1px solid var(--border-subtle)',
                            }}
                          >
                            <div>
                              <div className="text-sm font-semibold">
                                {dev.name}
                              </div>
                              <div
                                className="text-xs"
                                style={{ color: 'var(--text-muted)' }}
                              >
                                {dev.address} • {dev.signal}
                              </div>
                            </div>
                            <button
                              type="button"
                              onClick={() => handleConnectPeer(dev)}
                              className="rounded-lg bg-[#4A90E2] px-3 py-1.5 text-xs font-semibold text-white transition hover:brightness-110"
                            >
                              {dev.connected ? 'Open Chat' : 'Connect'}
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>

              {/* Bluetooth Devices Card */}
              <div
                className="rounded-2xl p-4 shadow-sm"
                style={{
                  backgroundColor: 'var(--surface-variant)',
                  border: '1px solid var(--border-subtle)',
                }}
              >
                <button
                  type="button"
                  onClick={() => setBtExpanded((v) => !v)}
                  className="flex w-full items-center justify-between text-left"
                >
                  <span className="text-[15px] font-bold">
                    Bluetooth Devices
                  </span>
                  {btExpanded ? (
                    <ChevronUp className="h-5 w-5" />
                  ) : (
                    <ChevronDown className="h-5 w-5" />
                  )}
                </button>

                {btExpanded && (
                  <div className="mt-3 flex flex-col gap-2">
                    <button
                      type="button"
                      onClick={handleToggleBtHost}
                      className={`w-full rounded-xl px-4 py-2.5 text-sm font-semibold text-white transition hover:brightness-110 ${
                        btHostActive ? 'bg-[#4A90E2]' : 'bg-[#8A2BE2]'
                      }`}
                    >
                      {btHostActive ? 'STOP BT HOST' : 'START BT HOST'}
                    </button>

                    <button
                      type="button"
                      onClick={handleScanBt}
                      disabled={btScanning}
                      className="w-full rounded-xl bg-[#8A2BE2]/85 px-4 py-2.5 text-sm font-semibold text-white transition hover:brightness-110 disabled:opacity-60"
                    >
                      {btScanning ? 'SCANNING BT DEVICES...' : 'SCAN BT DEVICES'}
                    </button>

                    {discoveredBt.length > 0 && (
                      <div className="mt-1 flex flex-col gap-2">
                        {discoveredBt.map((dev) => (
                          <div
                            key={dev.id}
                            className="flex items-center justify-between rounded-xl p-2.5"
                            style={{
                              backgroundColor: 'var(--surface)',
                              border: '1px solid var(--border-subtle)',
                            }}
                          >
                            <div>
                              <div className="text-sm font-semibold">
                                {dev.name}
                              </div>
                              <div
                                className="text-xs"
                                style={{ color: 'var(--text-muted)' }}
                              >
                                {dev.address} • {dev.signal}
                              </div>
                            </div>
                            <button
                              type="button"
                              onClick={() => handleConnectPeer(dev)}
                              className="rounded-lg bg-[#4A90E2] px-3 py-1.5 text-xs font-semibold text-white transition hover:brightness-110"
                            >
                              {dev.connected ? 'Open Chat' : 'Connect'}
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>

              {/* Manual IP Connection Card */}
              <div
                className="rounded-2xl p-4 shadow-sm"
                style={{
                  backgroundColor: 'var(--surface-variant)',
                  border: '1px solid var(--border-subtle)',
                }}
              >
                <input
                  type="text"
                  value={targetIp}
                  onChange={(e) => setTargetIp(e.target.value)}
                  placeholder="Target Host IP Address"
                  className="w-full border-b bg-transparent px-1 py-2 text-sm outline-none focus:border-[#8A2BE2]"
                  style={{ borderColor: 'var(--border-subtle)' }}
                />

                <div className="mt-3 flex gap-2">
                  <button
                    type="button"
                    onClick={handleHostTcp}
                    className={`flex-1 rounded-xl px-4 py-2.5 text-sm font-semibold text-white transition hover:brightness-110 ${
                      tcpHosting ? 'bg-[#4A90E2]' : 'bg-[#8A2BE2]'
                    }`}
                  >
                    {tcpHosting ? 'Hosting TCP' : 'Host TCP'}
                  </button>
                  <button
                    type="button"
                    onClick={handleConnectIp}
                    className="flex-1 rounded-xl bg-[#4A90E2] px-4 py-2.5 text-sm font-semibold text-white transition hover:brightness-110"
                  >
                    Connect IP
                  </button>
                </div>
              </div>
            </div>
          </section>
        )}

        {/* TAB 3: ACTIVE CHAT */}
        {activeTab === 'chat' && activeThread && (
          <section className="flex h-full flex-col pb-[74px]">
            {/* Reply Preview Bar */}
            {replyingTo && (
              <div
                className="flex items-center justify-between px-3 py-2"
                style={{ backgroundColor: 'var(--reply-bg)' }}
              >
                <span className="truncate text-[13px]">
                  Replying to <strong>{replyingTo.sender}</strong>:{' '}
                  {replyingTo.text}
                </span>
                <button
                  type="button"
                  onClick={() => setReplyingTo(null)}
                  aria-label="Cancel reply"
                  className="ml-2 rounded p-0.5 hover:bg-black/10"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
            )}

            {/* Messages RecyclerView */}
            <div className="flex-1 overflow-y-auto p-3">
              <div className="mx-auto flex max-w-3xl flex-col gap-2.5">
                {activeThread.messages.map((msg) => (
                  <div
                    key={msg.id}
                    className={`group flex flex-col ${
                      msg.isOwn ? 'items-end' : 'items-start'
                    }`}
                  >
                    <div
                      className={`max-w-[80%] rounded-2xl px-3.5 py-2.5 shadow-sm ${
                        msg.isOwn ? 'bg-[#8A2BE2] text-white' : ''
                      }`}
                      style={
                        msg.isOwn
                          ? undefined
                          : {
                              backgroundColor: 'var(--bubble-peer)',
                              color: 'var(--text-primary)',
                            }
                      }
                    >
                      {!msg.isOwn && (
                        <div className="mb-0.5 text-[11px] font-bold text-[#4A90E2]">
                          {msg.sender}
                        </div>
                      )}

                      {msg.replyTo && (
                        <div className="mb-1.5 rounded-lg bg-black/20 px-2.5 py-1 text-[11px] opacity-90">
                          <span className="font-semibold">
                            {msg.replyTo.sender}:{' '}
                          </span>
                          <span className="truncate">{msg.replyTo.text}</span>
                        </div>
                      )}

                      {msg.attachment && (
                        <div className="mb-1.5 flex items-center gap-1.5 rounded-lg bg-black/20 px-2.5 py-1.5 text-xs">
                          <Paperclip className="h-3.5 w-3.5 shrink-0" />
                          <span className="truncate font-medium">
                            {msg.attachment.name}
                          </span>
                          <span className="shrink-0 opacity-75">
                            ({msg.attachment.sizeLabel})
                          </span>
                        </div>
                      )}

                      <div className="whitespace-pre-wrap text-sm leading-snug">
                        {msg.text}
                      </div>

                      <div className="mt-1 flex items-center justify-end gap-2 text-[10px] opacity-75">
                        <span>{msg.timestamp}</span>
                        <button
                          type="button"
                          onClick={() => setReplyingTo(msg)}
                          className="inline-flex items-center gap-0.5 underline-offset-2 hover:underline"
                          title="Reply to message"
                        >
                          <Reply className="h-3 w-3" />
                          Reply
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
                <div ref={messagesEndRef} />
              </div>
            </div>

            {/* Pending attachment pill */}
            {pendingAttachment && (
              <div
                className="mx-3 mb-1 flex items-center justify-between rounded-xl px-3 py-1.5 text-xs"
                style={{
                  backgroundColor: 'var(--surface-variant)',
                  border: '1px solid var(--border-subtle)',
                }}
              >
                <span className="truncate">
                  Attached: <strong>{pendingAttachment.name}</strong> (
                  {pendingAttachment.sizeLabel})
                </span>
                <button
                  type="button"
                  onClick={() => setPendingAttachment(null)}
                  className="ml-2 text-[#8A2BE2]"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
            )}

            {/* Input Field & Attachments Bar */}
            <div
              className="flex items-center gap-1 px-2 py-2"
              style={{
                backgroundColor: 'var(--surface-variant)',
                borderTop: '1px solid var(--border-subtle)',
              }}
            >
              <input
                ref={fileInputRef}
                type="file"
                onChange={handleAttachmentChange}
                className="hidden"
              />
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                aria-label="Attach file"
                className="rounded-full p-2 text-[#8A2BE2] transition hover:bg-[#8A2BE2]/15"
              >
                <Plus className="h-5 w-5" />
              </button>

              <input
                type="text"
                value={messageInput}
                onChange={(e) => setMessageInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    handleSendMessage();
                  }
                }}
                placeholder="Type a message..."
                className="flex-1 bg-transparent px-2.5 py-2 text-sm outline-none"
              />

              <button
                type="button"
                onClick={handleSendMessage}
                aria-label="Send message"
                className="rounded-full p-2 text-[#4A90E2] transition hover:bg-[#4A90E2]/15"
              >
                <Send className="h-5 w-5" />
              </button>
            </div>
          </section>
        )}

        {/* TAB 4: SETTINGS */}
        {activeTab === 'settings' && (
          <section className="h-full overflow-y-auto p-4 pb-24">
            <div className="mx-auto flex max-w-3xl flex-col gap-4">
              {/* Account & Profile Card */}
              <div
                className="rounded-2xl p-4 shadow-sm"
                style={{
                  backgroundColor: 'var(--surface-variant)',
                  border: '1px solid var(--border-subtle)',
                }}
              >
                <h2 className="text-lg font-bold">Account &amp; Profile</h2>

                <button
                  type="button"
                  onClick={handleGoogleSignIn}
                  className="mt-3 w-full rounded-xl bg-[#4A90E2] px-4 py-2.5 text-sm font-semibold text-white transition hover:brightness-110"
                >
                  {isGoogleSignedIn
                    ? 'Signed In with Google (Disconnect)'
                    : 'Sign In with Google'}
                </button>

                {!isEditingUsername ? (
                  <div className="mt-3 flex items-center gap-2">
                    <span className="flex-1 text-base font-medium">
                      {username}
                    </span>
                    <button
                      type="button"
                      onClick={() => {
                        setUsernameDraft(username);
                        setIsEditingUsername(true);
                      }}
                      className="rounded-xl bg-[#8A2BE2] px-4 py-2 text-sm font-semibold text-white transition hover:brightness-110"
                    >
                      Edit
                    </button>
                  </div>
                ) : (
                  <div className="mt-3 flex flex-col gap-2">
                    <input
                      type="text"
                      value={usernameDraft}
                      onChange={(e) => setUsernameDraft(e.target.value)}
                      placeholder="Username"
                      className="w-full border-b bg-transparent px-1 py-2 text-sm outline-none focus:border-[#8A2BE2]"
                      style={{ borderColor: 'var(--border-subtle)' }}
                    />
                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={handleSaveUsername}
                        className="flex-1 rounded-xl bg-[#8A2BE2] px-4 py-2 text-sm font-semibold text-white transition hover:brightness-110"
                      >
                        Save
                      </button>
                      <button
                        type="button"
                        onClick={() => setIsEditingUsername(false)}
                        className="flex-1 rounded-xl px-4 py-2 text-sm font-semibold transition"
                        style={{
                          backgroundColor: 'var(--surface)',
                          border: '1px solid var(--border-subtle)',
                        }}
                      >
                        Cancel
                      </button>
                    </div>
                  </div>
                )}

                <button
                  type="button"
                  onClick={handleSaveProfile}
                  className="mt-3 w-full rounded-xl bg-[#8A2BE2] px-4 py-2.5 text-sm font-semibold text-white transition hover:brightness-110"
                >
                  Save Profile
                </button>
              </div>

              {/* App Theme Card */}
              <div
                className="rounded-2xl p-4 shadow-sm"
                style={{
                  backgroundColor: 'var(--surface-variant)',
                  border: '1px solid var(--border-subtle)',
                }}
              >
                <h2 className="text-lg font-bold">App Theme</h2>
                <div className="mt-3 flex gap-1.5">
                  {(['system', 'dark', 'light'] as ThemeMode[]).map((mode) => {
                    const selected = themeMode === mode;
                    return (
                      <button
                        key={mode}
                        type="button"
                        onClick={() => setThemeMode(mode)}
                        className={`flex-1 rounded-xl px-3 py-2.5 text-sm font-semibold capitalize transition ${
                          selected
                            ? 'bg-[#8A2BE2] text-white'
                            : 'hover:brightness-105'
                        }`}
                        style={
                          selected
                            ? undefined
                            : {
                                backgroundColor: 'var(--surface)',
                                border: '1px solid var(--border-subtle)',
                              }
                        }
                      >
                        {mode}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>
          </section>
        )}
      </main>

      {/* FLOATING PILL BOTTOM NAVIGATION DOCK (54dp height, #222438, 27dp radius) */}
      <nav
        aria-label="Primary Navigation"
        className="pointer-events-none fixed bottom-4 left-0 right-0 z-20 flex justify-center"
      >
        <div
          className="pointer-events-auto flex h-[54px] items-center gap-1.5 rounded-[27px] px-3 shadow-xl"
          style={{ backgroundColor: 'var(--dock-bg)' }}
        >
          <button
            type="button"
            onClick={() => switchTab('home')}
            aria-label="Spaces and Home"
            className={`flex h-[38px] w-[72px] items-center justify-center rounded-full transition ${
              activeTab === 'home' || activeTab === 'chat'
                ? 'bg-[#8A2BE2] text-white'
                : 'text-[#CCCCCC] hover:text-white'
            }`}
          >
            <Compass className="h-[22px] w-[22px]" />
          </button>

          <button
            type="button"
            onClick={() => switchTab('connections')}
            aria-label="Connections"
            className={`flex h-[38px] w-[72px] items-center justify-center rounded-full transition ${
              activeTab === 'connections'
                ? 'bg-[#8A2BE2] text-white'
                : 'text-[#CCCCCC] hover:text-white'
            }`}
          >
            <Share2 className="h-[22px] w-[22px]" />
          </button>

          <button
            type="button"
            onClick={() => switchTab('settings')}
            aria-label="Settings"
            className={`flex h-[38px] w-[72px] items-center justify-center rounded-full transition ${
              activeTab === 'settings'
                ? 'bg-[#8A2BE2] text-white'
                : 'text-[#CCCCCC] hover:text-white'
            }`}
          >
            <Settings className="h-[22px] w-[22px]" />
          </button>
        </div>
      </nav>

      {/* LEFT SIDEBAR DRAWER MENU (280dp) */}
      {drawerOpen && (
        <div className="fixed inset-0 z-40 flex">
          <div
            className="fixed inset-0 bg-black/50"
            onClick={() => setDrawerOpen(false)}
          />
          <aside
            className="relative z-10 flex h-full w-[280px] flex-col p-4 shadow-2xl"
            style={{
              backgroundColor: 'var(--surface-variant)',
              color: 'var(--text-primary)',
            }}
          >
            <div className="mt-6 text-2xl font-bold">TChat</div>
            <div className="mt-1 text-[13px] font-medium text-[#8A2BE2]">
              Logged in as: {username}
            </div>

            <div
              className="my-4 h-px w-full"
              style={{ backgroundColor: 'var(--border-subtle)' }}
            />

            <button
              type="button"
              onClick={() => switchTab('home')}
              className="rounded-lg p-3 text-left text-base transition hover:bg-[#8A2BE2]/15"
            >
              💬 Spaces &amp; Home
            </button>
            <button
              type="button"
              onClick={() => switchTab('connections')}
              className="rounded-lg p-3 text-left text-base transition hover:bg-[#8A2BE2]/15"
            >
              📡 Local &amp; BT Connections
            </button>
            <button
              type="button"
              onClick={() => switchTab('settings')}
              className="rounded-lg p-3 text-left text-base transition hover:bg-[#8A2BE2]/15"
            >
              ⚙️ App Settings
            </button>

            <div className="flex-1" />

            <button
              type="button"
              onClick={handleSignOut}
              className="w-full rounded-xl bg-[#8A2BE2] px-4 py-2.5 text-sm font-semibold text-white transition hover:brightness-110"
            >
              Sign Out
            </button>
          </aside>
        </div>
      )}

      {/* NEW CHAT OR SPACE MODAL (triggered by fabNewChat) */}
      {newChatModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <form
            onSubmit={handleCreateChat}
            className="w-full max-w-sm rounded-2xl p-5 shadow-2xl"
            style={{
              backgroundColor: 'var(--surface-variant)',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold">New Chat or Space</h3>
              <button
                type="button"
                onClick={() => setNewChatModalOpen(false)}
                className="rounded p-1 hover:bg-black/10"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="mt-4">
              <label className="text-xs font-semibold uppercase tracking-wide opacity-70">
                Name / Channel
              </label>
              <input
                type="text"
                value={newChatTitle}
                onChange={(e) => setNewChatTitle(e.target.value)}
                placeholder="e.g. #mesh-lobby or Jordan"
                autoFocus
                className="mt-1 w-full rounded-xl border bg-transparent px-3 py-2 text-sm outline-none focus:border-[#8A2BE2]"
                style={{ borderColor: 'var(--border-subtle)' }}
              />
            </div>

            <div className="mt-3">
              <label className="text-xs font-semibold uppercase tracking-wide opacity-70">
                Connection Mode
              </label>
              <div className="mt-1.5 grid grid-cols-2 gap-1.5">
                {(
                  ['Space', 'Local Wi-Fi', 'Bluetooth', 'TCP IP'] as ConnectionMode[]
                ).map((mode) => (
                  <button
                    key={mode}
                    type="button"
                    onClick={() => setNewChatMode(mode)}
                    className={`rounded-xl px-3 py-2 text-xs font-semibold transition ${
                      newChatMode === mode ? 'bg-[#8A2BE2] text-white' : ''
                    }`}
                    style={
                      newChatMode === mode
                        ? undefined
                        : {
                            backgroundColor: 'var(--surface)',
                            border: '1px solid var(--border-subtle)',
                          }
                    }
                  >
                    {mode}
                  </button>
                ))}
              </div>
            </div>

            <div className="mt-5 flex gap-2">
              <button
                type="button"
                onClick={() => setNewChatModalOpen(false)}
                className="flex-1 rounded-xl px-4 py-2 text-sm font-semibold"
                style={{
                  backgroundColor: 'var(--surface)',
                  border: '1px solid var(--border-subtle)',
                }}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="flex-1 rounded-xl bg-[#8A2BE2] px-4 py-2 text-sm font-semibold text-white transition hover:brightness-110"
              >
                Create
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
