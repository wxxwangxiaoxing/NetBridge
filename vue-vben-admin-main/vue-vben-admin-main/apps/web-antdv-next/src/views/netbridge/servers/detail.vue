<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Button, Card, Descriptions, Space, Switch, Table, Tag, Timeline } from 'antdv-next';

import { useNetbridgeRealtime } from '#/api/netbridge/realtime';
import { getServerDetail } from '#/api/netbridge/server';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const detail = ref<any | null>(null);
const autoRefresh = ref(true);
let autoRefreshTimer: null | ReturnType<typeof setInterval> = null;

const serverId = computed(() => Number(route.params.id));

const serviceColumns = [
  { dataIndex: 'name', key: 'name', title: '服务' },
  { dataIndex: 'type', key: 'type', title: '类型' },
  { dataIndex: 'status', key: 'status', title: '状态' },
  { dataIndex: 'healthStatus', key: 'healthStatus', title: '健康度' },
  { dataIndex: 'accessUrl', key: 'accessUrl', title: '访问地址' },
];

const taskColumns = [
  { dataIndex: 'type', key: 'type', title: '任务类型' },
  { dataIndex: 'status', key: 'status', title: '状态' },
  { dataIndex: 'createdAt', key: 'createdAt', title: '创建时间' },
  { dataIndex: 'completedAt', key: 'completedAt', title: '完成时间' },
];

const logColumns = [
  { dataIndex: 'level', key: 'level', title: '级别' },
  { dataIndex: 'source', key: 'source', title: '来源' },
  { dataIndex: 'content', key: 'content', title: '内容' },
  { dataIndex: 'createdAt', key: 'createdAt', title: '时间' },
];

const activityTimeline = computed(() => {
  const detailValue = detail.value;
  if (!detailValue) return [];
  const serviceEvents = (detailValue.services || []).slice(0, 8).map((service: any) => ({
    color: service.healthStatus === 'error' || service.status === 'error' ? 'red' : service.status === 'active' ? 'green' : 'gray',
    time: service.lastCheckAt || detailValue.lastHeartbeat || '',
    title: `service ${service.name}`,
    description: `${service.status || '--'} · ${service.healthStatus || 'unknown'} · ${service.accessUrl || 'no access url'}`,
  }));
  const taskEvents = (detailValue.recentTasks || []).map((task: any) => ({
    color: task.status === 'failed' || task.status === 'error' ? 'red' : task.status === 'running' ? 'blue' : 'green',
    time: task.completedAt || task.createdAt,
    title: `${task.type} ${task.status}`,
    description: `task #${task.id} · serviceId ${task.serviceId || '-'}`,
  }));
  const logEvents = (detailValue.recentLogs || []).slice(0, 8).map((log: any) => ({
    color: log.level === 'ERROR' ? 'red' : log.level === 'WARN' ? 'orange' : 'gray',
    time: log.createdAt,
    title: `${log.level} ${log.source}`,
    description: log.content,
  }));
  return [...serviceEvents, ...taskEvents, ...logEvents]
    .filter((item) => item.time)
    .sort((a, b) => new Date(b.time).getTime() - new Date(a.time).getTime())
    .slice(0, 14);
});

function tagColor(status?: string) {
  switch (status) {
    case 'online':
    case 'active':
    case 'healthy':
      return 'success';
    case 'error':
      return 'error';
    case 'warning':
      return 'warning';
    default:
      return 'default';
  }
}

async function loadDetail() {
  if (!serverId.value) return;
  loading.value = true;
  try {
    detail.value = await getServerDetail(serverId.value);
  } finally {
    loading.value = false;
  }
}

function startAutoRefresh() {
  if (autoRefreshTimer || !autoRefresh.value) return;
  autoRefreshTimer = setInterval(() => {
    loadDetail();
  }, 15000);
}

function stopAutoRefresh() {
  if (!autoRefreshTimer) return;
  clearInterval(autoRefreshTimer);
  autoRefreshTimer = null;
}

const { connectionLabel, lastMessage } = useNetbridgeRealtime({
  onServerStatus(payload) {
    if (!detail.value) return;
    if (detail.value.id !== payload.serverId && detail.value.id !== payload.entityId) return;
    detail.value = {
      ...detail.value,
      lastHeartbeat: payload.timestamp || detail.value.lastHeartbeat,
      status: payload.status || detail.value.status,
    };
  },
  onServiceStatus(payload) {
    if (!detail.value?.services?.length) return;
    const target = detail.value.services.find(
      (item: any) => item.id === payload.serviceId || item.id === payload.entityId,
    );
    if (!target) return;
    target.status = payload.status || target.status;
    target.healthStatus = payload.healthStatus || target.healthStatus;
  },
  onTaskStatus(payload) {
    if (!detail.value?.recentTasks?.length) return;
    const target = detail.value.recentTasks.find(
      (item: any) => item.id === payload.taskId || item.id === payload.entityId,
    );
    if (!target) return;
    target.status = payload.status || target.status;
  },
});

onMounted(loadDetail);

watch(
  autoRefresh,
  (enabled) => {
    if (enabled) {
      startAutoRefresh();
    } else {
      stopAutoRefresh();
    }
  },
  { immediate: true },
);

onBeforeUnmount(() => {
  stopAutoRefresh();
});
</script>

<template>
  <div class="p-5">
    <div class="mb-4 flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-semibold text-slate-900">服务器详情</h2>
        <p class="mt-1 text-sm text-slate-500">围绕单台 Agent 节点查看服务、任务与日志聚合信息。</p>
      </div>
      <Space>
        <Tag :color="connectionLabel === 'live' ? 'success' : connectionLabel === 'connecting' ? 'processing' : 'default'">
          {{ connectionLabel }}
        </Tag>
        <Tag v-if="lastMessage?.message" color="cyan">{{ lastMessage.message }}</Tag>
        <Space size="small">
          <span class="text-xs text-slate-500">自动刷新</span>
          <Switch v-model:checked="autoRefresh" size="small" />
        </Space>
        <Button @click="loadDetail">手动刷新</Button>
        <Button @click="router.push('/netbridge/servers')">返回列表</Button>
      </Space>
    </div>

    <div class="mb-5 grid gap-4 md:grid-cols-3">
      <Card class="rounded-2xl border-0 shadow-sm">
        <div class="text-xs uppercase tracking-[0.2em] text-slate-500">节点状态</div>
        <div class="mt-4">
          <Tag :color="tagColor(detail?.status)">{{ detail?.status || '--' }}</Tag>
        </div>
        <div class="mt-4 text-2xl font-semibold text-slate-900">{{ detail?.hostname || '--' }}</div>
        <div class="mt-1 text-sm text-slate-500">{{ detail?.ip || '--' }}</div>
      </Card>
      <Card class="rounded-2xl border-0 shadow-sm">
        <div class="text-xs uppercase tracking-[0.2em] text-slate-500">聚合统计</div>
        <div class="mt-4 space-y-2 text-sm text-slate-600">
          <div>服务总数：<span class="font-medium text-slate-900">{{ detail?.serviceTotal ?? 0 }}</span></div>
          <div>最近任务：<span class="font-medium text-slate-900">{{ detail?.recentTaskTotal ?? 0 }}</span></div>
          <div>最近日志：<span class="font-medium text-slate-900">{{ detail?.recentLogTotal ?? 0 }}</span></div>
        </div>
      </Card>
      <Card class="rounded-2xl border-0 shadow-sm">
        <div class="text-xs uppercase tracking-[0.2em] text-slate-500">网络信息</div>
        <div class="mt-4 space-y-2 text-sm text-slate-600">
          <div>Tailscale IP：<span class="font-medium text-slate-900">{{ detail?.tailscaleIp || '--' }}</span></div>
          <div>用户 ID：<span class="font-medium text-slate-900">{{ detail?.userId ?? '--' }}</span></div>
        </div>
      </Card>
    </div>

    <Card class="mb-5 rounded-2xl border-0 shadow-sm" :loading="loading">
      <Descriptions :column="2" bordered size="small" title="基础信息">
        <Descriptions.Item label="主机名">{{ detail?.hostname || '--' }}</Descriptions.Item>
        <Descriptions.Item label="状态">
          <Tag :color="tagColor(detail?.status)">{{ detail?.status || '--' }}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="IP">{{ detail?.ip || '--' }}</Descriptions.Item>
        <Descriptions.Item label="Tailscale IP">{{ detail?.tailscaleIp || '--' }}</Descriptions.Item>
      </Descriptions>
    </Card>

    <Card class="mb-5 rounded-2xl border-0 shadow-sm" title="服务列表">
      <Table :columns="serviceColumns" :data-source="detail?.services || []" :pagination="false" :row-key="(record: any) => record.id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <button class="font-medium text-left text-sky-600 hover:text-sky-500" @click="router.push(`/netbridge/services/${record.id}`)">
              {{ record.name }}
            </button>
          </template>
          <template v-else-if="column.key === 'status' || column.key === 'healthStatus'">
            <Tag :color="tagColor(record[column.key])">{{ record[column.key] }}</Tag>
          </template>
          <template v-else-if="column.key === 'accessUrl'">
            <div class="max-w-[320px] truncate text-slate-600">{{ record.accessUrl || '--' }}</div>
          </template>
        </template>
      </Table>
    </Card>

    <Card class="mb-5 rounded-2xl border-0 shadow-sm" title="最近任务">
      <Table :columns="taskColumns" :data-source="detail?.recentTasks || []" :pagination="false" :row-key="(record: any) => record.id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <Tag :color="tagColor(record.status)">{{ record.status }}</Tag>
          </template>
        </template>
      </Table>
    </Card>

    <Card class="rounded-2xl border-0 shadow-sm" title="最近日志">
      <Table :columns="logColumns" :data-source="detail?.recentLogs || []" :pagination="false" :row-key="(record: any) => record.id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'level'">
            <Tag :color="record.level === 'ERROR' ? 'error' : record.level === 'WARN' ? 'warning' : 'default'">
              {{ record.level }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'content'">
            <div class="max-w-[420px] truncate text-slate-600">{{ record.content }}</div>
          </template>
        </template>
      </Table>
    </Card>

    <Card class="mt-5 rounded-2xl border-0 shadow-sm" title="节点轨迹">
      <Timeline>
        <Timeline.Item
          v-for="item in activityTimeline"
          :key="`${item.time}-${item.title}`"
          :color="item.color"
        >
          <div class="text-sm font-medium text-slate-900">{{ item.title }}</div>
          <div class="mt-1 text-xs text-slate-500">{{ item.time }}</div>
          <div class="mt-2 text-sm text-slate-600">{{ item.description }}</div>
        </Timeline.Item>
      </Timeline>
    </Card>
  </div>
</template>
