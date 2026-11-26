export type TabId = 'home' | 'connections' | 'chat' | 'settings';

export type ThemeMode = 'system' | 'dark' | 'light';

export type ConnectionMode = 'Space' | 'Local Wi-Fi' | 'Bluetooth' | 'TCP IP';

export interface MessageAttachment {
  name: string;
  sizeLabel: string;
}

export interface ReplyReference {
  id: string;
  sender: string;
  text: string;
}

export interface ChatMessage {
  id: string;
  sender: string;
  text: string;
  timestamp: string;
  isOwn: boolean;
  replyTo?: ReplyReference;
  attachment?: MessageAttachment;
}

export interface ChatThread {
  id: string;
  title: string;
  mode: ConnectionMode;
  endpoint?: string;
  unreadCount: number;
  messages: ChatMessage[];
}

export interface DiscoveredDevice {
  id: string;
  name: string;
  address: string;
  kind: 'wifi' | 'bluetooth';
  signal: string;
  connected: boolean;
}

export interface BannerNotification {
  id: number;
  sender: string;
  message: string;
}
