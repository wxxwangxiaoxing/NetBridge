import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

import { useAccessStore } from '@vben/stores';

export interface NetbridgeStatusMessage {
  entityId?: number;
  healthStatus?: string | null;
  message?: string | null;
  serverId?: number;
  serviceId?: number;
  status?: string | null;
  taskId?: number;
  timestamp?: string | null;
  type?: string | null;
}

interface UseNetbridgeRealtimeOptions {
  onServerStatus?: (payload: NetbridgeStatusMessage) => void;
  onServiceStatus?: (payload: NetbridgeStatusMessage) => void;
  onTaskStatus?: (payload: NetbridgeStatusMessage) => void;
}

const DESTINATIONS = [
  '/user/queue/server/status',
  '/user/queue/task/status',
  '/user/queue/service/status',
];

export function useNetbridgeRealtime(options: UseNetbridgeRealtimeOptions = {}) {
  const accessStore = useAccessStore();
  const socket = ref<null | WebSocket>(null);
  const isConnected = ref(false);
  const isConnecting = ref(false);
  const lastMessage = ref<NetbridgeStatusMessage | null>(null);

  let reconnectTimer: null | ReturnType<typeof setTimeout> = null;
  let heartbeatTimer: null | ReturnType<typeof setInterval> = null;
  let frameBuffer = '';
  let isDisposed = false;

  const connectionLabel = computed(() => {
    if (isConnected.value) return 'live';
    if (isConnecting.value) return 'connecting';
    return 'offline';
  });

  function buildWsUrl() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${window.location.host}/ws`;
  }

  function clearTimers() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer);
      heartbeatTimer = null;
    }
  }

  function sendFrame(command: string, headers: Record<string, string> = {}, body = '') {
    if (!socket.value || socket.value.readyState !== WebSocket.OPEN) {
      return;
    }
    const headerLines = Object.entries(headers).map(([key, value]) => `${key}:${value}`);
    const frame = `${command}\n${headerLines.join('\n')}\n\n${body}\0`;
    socket.value.send(frame);
  }

  function startHeartbeat() {
    heartbeatTimer = setInterval(() => {
      if (socket.value?.readyState === WebSocket.OPEN) {
        socket.value.send('\n');
      }
    }, 15000);
  }

  function subscribeTopics() {
    DESTINATIONS.forEach((destination, index) => {
      sendFrame('SUBSCRIBE', {
        ack: 'auto',
        destination,
        id: `netbridge-sub-${index + 1}`,
      });
    });
  }

  function routeMessage(destination: string | undefined, payload: NetbridgeStatusMessage) {
    lastMessage.value = payload;
    if (destination?.includes('/server/status') || payload.type === 'server_status') {
      options.onServerStatus?.(payload);
      return;
    }
    if (destination?.includes('/task/status') || payload.type === 'task_status') {
      options.onTaskStatus?.(payload);
      return;
    }
    if (destination?.includes('/service/status') || payload.type === 'service_status') {
      options.onServiceStatus?.(payload);
    }
  }

  function parseFrame(frame: string) {
    const normalized = frame.replace(/\r/g, '');
    if (!normalized.trim()) return;

    const [headerPart, ...bodyParts] = normalized.split('\n\n');
    const headerLines = headerPart.split('\n');
    const command = headerLines.shift()?.trim();
    const headers = Object.fromEntries(
      headerLines
        .filter(Boolean)
        .map((line) => {
          const separatorIndex = line.indexOf(':');
          return [line.slice(0, separatorIndex), line.slice(separatorIndex + 1)];
        }),
    );
    const body = bodyParts.join('\n\n').trim();

    if (command === 'CONNECTED') {
      isConnecting.value = false;
      isConnected.value = true;
      subscribeTopics();
      startHeartbeat();
      return;
    }

    if (command === 'MESSAGE') {
      if (!body) return;
      try {
        const payload = JSON.parse(body) as NetbridgeStatusMessage;
        routeMessage(headers.destination, payload);
      } catch (error) {
        console.warn('failed to parse netbridge realtime message', error);
      }
      return;
    }

    if (command === 'ERROR') {
      console.warn('netbridge realtime error', body || headers.message);
    }
  }

  function handleRawMessage(raw: string) {
    frameBuffer += raw;
    let boundaryIndex = frameBuffer.indexOf('\0');
    while (boundaryIndex >= 0) {
      const frame = frameBuffer.slice(0, boundaryIndex);
      frameBuffer = frameBuffer.slice(boundaryIndex + 1);
      parseFrame(frame);
      boundaryIndex = frameBuffer.indexOf('\0');
    }
  }

  function scheduleReconnect() {
    if (isDisposed || reconnectTimer) return;
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null;
      connect();
    }, 3000);
  }

  function connect() {
    if (isDisposed || socket.value || !accessStore.accessToken) {
      return;
    }
    isConnecting.value = true;
    frameBuffer = '';
    const ws = new WebSocket(buildWsUrl());
    socket.value = ws;

    ws.onopen = () => {
      const token = accessStore.accessToken;
      sendFrame('CONNECT', {
        'accept-version': '1.2',
        'heart-beat': '10000,10000',
        Authorization: `Bearer ${token}`,
        token: token ?? '',
      });
    };

    ws.onmessage = (event) => {
      if (typeof event.data === 'string') {
        handleRawMessage(event.data);
      }
    };

    ws.onclose = () => {
      clearTimers();
      socket.value = null;
      isConnecting.value = false;
      isConnected.value = false;
      scheduleReconnect();
    };

    ws.onerror = () => {
      ws.close();
    };
  }

  function disconnect() {
    clearTimers();
    const current = socket.value;
    socket.value = null;
    isConnecting.value = false;
    isConnected.value = false;
    if (current && current.readyState === WebSocket.OPEN) {
      sendFrame('DISCONNECT');
      current.close();
    } else {
      current?.close();
    }
  }

  onMounted(() => {
    isDisposed = false;
    connect();
  });

  onBeforeUnmount(() => {
    isDisposed = true;
    disconnect();
  });

  return {
    connectionLabel,
    isConnected,
    isConnecting,
    lastMessage,
    reconnect: connect,
  };
}
