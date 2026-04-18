<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Button, Card, Descriptions, Space, Switch, Table, Tag, Timeline } from 'antdv-next';

import { useNetbridgeRealtime } from '#/api/netbridge/realtime';
import { getServiceDetail } from '#/api/netbridge/service';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const detail = ref<any | null>(null);
const autoRefresh = ref(true);
let autoRefreshTimer: null | ReturnType<typeof setInterval> = null;

const serviceId = computed(() => Number(route.params.id));

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

const metricColumns = [
  { dataIndex: 'status', key: 'status', title: '状态' },
  { dataIndex: 'responseTimeMs', key: 'responseTimeMs', title: '响应时间' },
  { dataIndex: 'successCount', key: 'successCount', title: '成功次数' },
  { dataIndex: 'errorCount', key: 'errorCount', title: '失败次数' },
  { dataIndex: 'collectedAt', key: 'collectedAt', title: '采集时间' },
];

const activityTimeline = computed(() => {
  const detailValue = detail.value;
  if (!detailValue) return [];
  const taskEvents = (detailValue.recentTasks || []).map((task: any) => ({
    color: task.status === 'failed' || task.status === 'error' ? 'red' : task.status === 'running' ? 'blue' : 'green',
    time: task.completedAt || task.createdAt,
    title: `${task.type} ${task.status}`,
    description: `task #${task.id} · serviceId ${task.serviceId || '-'}`,
  }));
  const logEvents = (detailValue.recentLogs || []).slice(0, 6).map((log: any) => ({
    color: log.level === 'ERROR' ? 'red' : log.level === 'WARN' ? 'orange' : 'gray',
    time: log.createdAt,
    title: `${log.level} ${log.source}`,
    description: log.content,
  }));
  const metricEvents = (detailValue.metrics || []).slice(0, 6).map((metric: any) => ({
    color: metric.status === 1 ? 'green' : metric.status === 0 ? 'gray' : 'red',
    time: metric.collectedAt,
    title: `check ${metric.status === 1 ? 'healthy' : metric.status === 0 ? 'unknown' : 'error'}`,
    description: `response ${metric.responseTimeMs != null ? `${metric.responseTimeMs} ms` : '--'} · success ${metric.successCount ?? '--'} · error ${metric.errorCount ?? '--'}`,
  }));

  return [...taskEvents, ...logEvents, ...metricEvents]
    .filter((item) => item.time)
    .sort((a, b) => new Date(b.time).getTime() - new Date(a.time).getTime())
    .slice(0, 12);
});

function tagColor(status?: string) {
  switch (status) {
    case 'active':
    case 'healthy':
      return 'success';
    case 'error':
      return 'error';
    case 'warning':
      return 'warning';
    case 'inactive':
      return 'default';
    default:
      return 'processing';
  }
}

async function loadDetail() {
  if (!serviceId.value) return;
  loading.value = true;
  try {
    detail.value = await getServiceDetail(serviceId.value);
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
  onServiceStatus(payload) {
    if (!detail.value) return;
    if (detail.value.id !== payload.serviceId && detail.value.id !== payload.entityId) return;
    detail.value = {
      ...detail.value,
      healthStatus: payload.healthStatus || detail.value.healthStatus,
      status: payload.status || detail.value.status,
    };
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
        <h2 class="text-2xl font-semibold text-slate-900">服务详情</h2>
        <p class="mt-1 text-sm text-slate-500">聚合访问地址、健康状态、最近任务、日志与最新检测指标。</p>
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
        <Button @click="router.push(`/netbridge/servers/${detail?.serverId}`)">查看服务器</Button>
        <Button @click="router.push('/netbridge/services')">返回列表</Button>
      </Space>
    </div>

    <div class="mb-5 grid gap-4 md:grid-cols-3">
      <Card class="rounded-2xl border-0 shadow-sm">
        <div class="text-xs uppercase tracking-[0.2em] text-slate-500">运行状态</div>
        <div class="mt-4 flex flex-wrap gap-2">
          <Tag :color="tagColor(detail?.status)">{{ detail?.status || '--' }}</Tag>
          <Tag :color="tagColor(detail?.healthStatus)">{{ detail?.healthStatus || '--' }}</Tag>
        </div>
        <div class="mt-4 text-2xl font-semibold text-slate-900">{{ detail?.name || '--' }}</div>
        <div class="mt-1 text-sm text-slate-500">{{ detail?.type || '--' }}</div>
      </Card>
      <Card class="rounded-2xl border-0 shadow-sm">
        <div class="text-xs uppercase tracking-[0.2em] text-slate-500">访问概览</div>
        <div class="mt-4 space-y-2 text-sm text-slate-600">
          <div>协议：<span class="font-medium text-slate-900">{{ detail?.protocol || '--' }}</span></div>
          <div>端口：<span class="font-medium text-slate-900">{{ detail?.port ?? '--' }}</span></div>
          <div>接入方式：<span class="font-medium text-slate-900">{{ detail?.accessType || '--' }}</span></div>
        </div>
      </Card>
      <Card class="rounded-2xl border-0 shadow-sm">
        <div class="text-xs uppercase tracking-[0.2em] text-slate-500">聚合统计</div>
        <div class="mt-4 space-y-2 text-sm text-slate-600">
          <div>最近任务：<span class="font-medium text-slate-900">{{ detail?.recentTaskTotal ?? 0 }}</span></div>
          <div>最近日志：<span class="font-medium text-slate-900">{{ detail?.recentLogTotal ?? 0 }}</span></div>
          <div>监控样本：<span class="font-medium text-slate-900">{{ detail?.metrics?.length ?? 0 }}</span></div>
        </div>
      </Card>
    </div>

    <Card class="mb-5 rounded-2xl border-0 shadow-sm" :loading="loading">
      <Descriptions :column="2" bordered size="small" title="基础信息">
        <Descriptions.Item label="服务名">{{ detail?.name || '--' }}</Descriptions.Item>
        <Descriptions.Item label="服务类型">{{ detail?.type || '--' }}</Descriptions.Item>
        <Descriptions.Item label="目标地址">{{ detail?.targetHost || '--' }}</Descriptions.Item>
        <Descriptions.Item label="访问地址">
          <div class="max-w-[420px] break-all text-slate-700">{{ detail?.accessUrl || '--' }}</div>
        </Descriptions.Item>
        <Descriptions.Item label="域名">{{ detail?.domain || '--' }}</Descriptions.Item>
        <Descriptions.Item label="服务器 ID">{{ detail?.serverId ?? '--' }}</Descriptions.Item>
      </Descriptions>
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

    <Card class="mb-5 rounded-2xl border-0 shadow-sm" title="最近日志">
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

    <Card class="mb-5 rounded-2xl border-0 shadow-sm" title="执行轨迹">
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

    <Card class="rounded-2xl border-0 shadow-sm" title="最近监控样本">
      <Table :columns="metricColumns" :data-source="detail?.metrics || []" :pagination="false" :row-key="(record: any) => record.id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === 1 ? 'success' : record.status === 0 ? 'default' : 'error'">
              {{ record.status === 1 ? 'healthy' : record.status === 0 ? 'unknown' : 'error' }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'responseTimeMs'">
            {{ record.responseTimeMs != null ? `${record.responseTimeMs} ms` : '--' }}
          </template>
        </template>
      </Table>
    </Card>
  </div>
</template>
