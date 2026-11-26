import { ChatThread, DiscoveredDevice } from '../types';

export const INITIAL_THREADS: ChatThread[] = [];

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
