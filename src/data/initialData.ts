import { ChatThread, DiscoveredDevice } from '../types';

export const INITIAL_THREADS: ChatThread[] = [
  {
    id: 'space-general',
    title: '#general',
    mode: 'Space',
    endpoint: 'Local Mesh Space',
    unreadCount: 2,
    messages: [
      {
        id: 'm-1',
        sender: 'Alex',
        text: 'Welcome to TChat! Ready to test Spaces and peer links.',
        timestamp: '09:15',
        isOwn: false,
      },
      {
        id: 'm-2',
        sender: 'Maya',
        text: 'Local router discovery is up on 192.168.0.x.',
        timestamp: '09:18',
        isOwn: false,
      },
    ],
  },
  {
    id: 'dm-alex',
    title: 'Alex Rivera',
    mode: 'Local Wi-Fi',
    endpoint: '192.168.0.104:8888',
    unreadCount: 0,
    messages: [
      {
        id: 'm-3',
        sender: 'Alex Rivera',
        text: 'Ping me when your TCP host is listening.',
        timestamp: 'Yesterday',
        isOwn: false,
      },
    ],
  },
  {
    id: 'space-dev',
    title: '#bluetooth-mesh',
    mode: 'Bluetooth',
    endpoint: 'BT RFCOMM Channel',
    unreadCount: 1,
    messages: [
      {
        id: 'm-4',
        sender: 'Sam',
        text: 'BT host advertising started nearby.',
        timestamp: 'Yesterday',
        isOwn: false,
      },
    ],
  },
];

export const MOCK_WIFI_DEVICES: DiscoveredDevice[] = [
  {
    id: 'wifi-1',
    name: 'Pixel-8-Pro (TChat)',
    address: '192.168.0.104',
    kind: 'wifi',
    signal: 'Strong (-48 dBm)',
    connected: false,
  },
  {
    id: 'wifi-2',
    name: 'Workstation-Node',
    address: '192.168.0.118',
    kind: 'wifi',
    signal: 'Good (-61 dBm)',
    connected: false,
  },
  {
    id: 'wifi-3',
    name: 'Tablet-Relay',
    address: '192.168.0.142',
    kind: 'wifi',
    signal: 'Fair (-73 dBm)',
    connected: false,
  },
];

export const MOCK_BT_DEVICES: DiscoveredDevice[] = [
  {
    id: 'bt-1',
    name: 'Galaxy S24 (TChat BT)',
    address: 'A4:83:E7:21:9C:04',
    kind: 'bluetooth',
    signal: '-52 dBm',
    connected: false,
  },
  {
    id: 'bt-2',
    name: 'Nothing Phone (2)',
    address: '7C:2A:31:5B:10:8F',
    kind: 'bluetooth',
    signal: '-67 dBm',
    connected: false,
  },
];
