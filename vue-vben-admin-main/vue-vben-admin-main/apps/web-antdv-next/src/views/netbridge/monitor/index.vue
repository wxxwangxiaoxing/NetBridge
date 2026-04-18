<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';

import { Button, Card, InputNumber, Select, Space, Table, Tabs, Tag } from 'antdv-next';

import { useNetbridgeRealtime } from '#/api/netbridge/realtime';
import { getServerMetricList, getServiceMetricList } from '#/api/netbridge/monitor';

const loading = ref(false);
const activeKey = ref('server');
const timeRangeHours = ref(24);

const serverRows = ref<any[]>([]);
const serverTotal = ref(0);
const serverQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  serverId: undefined as number | undefined,
});

const serviceRows = ref<any[]>([]);
const serviceTotal = ref(0);
const serviceQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  serverId: undefined as number | undefined,
  serviceId: undefined as number | undefined,
});

const serverColumns = [
  { dataIndex: 'serverId', key: 'serverId', title: '服务器 ID', width: 110 },
  { dataIndex: 'cpuUsage', key: 'cpuUsage', title: 'CPU' },
  { dataIndex: 'memoryUsage', key: 'memoryUsage', title: '内存' },
  { dataIndex: 'diskUsage', key: 'diskUsage', title: '磁盘' },
  { dataIndex: 'loadAverage', key: 'loadAverage', title: '负载' },
  { dataIndex: 'networkInBytes', key: 'networkInBytes', title: '入口流量' },
  { dataIndex: 'networkOutBytes', key: 'networkOutBytes', title: '出口流量' },
  { dataIndex: 'collectedAt', key: 'collectedAt', title: '采集时间', width: 180 },
];

const serviceColumns = [
  { dataIndex: 'serviceId', key: 'serviceId', title: '服务 ID', width: 110 },
  { dataIndex: 'serverId', key: 'serverId', title: '服务器 ID', width: 110 },
  { dataIndex: 'status', key: 'status', title: '状态' },
  { dataIndex: 'responseTimeMs', key: 'responseTimeMs', title: '响应时间' },
  { dataIndex: 'successCount', key: 'successCount', title: '成功次数' },
  { dataIndex: 'errorCount', key: 'errorCount', title: '失败次数' },
  { dataIndex: 'collectedAt', key: 'collectedAt', title: '采集时间', width: 180 },
];

const filteredServerRows = computed(() => {
  const cutoff = Date.now() - timeRangeHours.value * 60 * 60 * 1000;
  return serverRows.value.filter((item) => {
    const timestamp = item.collectedAt ? new Date(item.collectedAt).getTime() : 0;
    return !timestamp || timestamp >= cutoff;
  });
});

const filteredServiceRows = computed(() => {
  const cutoff = Date.now() - timeRangeHours.value * 60 * 60 * 1000;
  return serviceRows.value.filter((item) => {
    const timestamp = item.collectedAt ? new Date(item.collectedAt).getTime() : 0;
    return !timestamp || timestamp >= cutoff;
  });
});

const summary = computed(() => {
  const latestServer = filteredServerRows.value[0];
  const latestService = filteredServiceRows.value[0];
  return [
    {
      label: '最新 CPU',
      value: latestServer?.cpuUsage != null ? `${latestServer.cpuUsage}%` : '--',
      tone: 'from-cyan-500/15 to-blue-500/10',
    },
    {
      label: '最新内存',
      value: latestServer?.memoryUsage != null ? `${latestServer.memoryUsage}%` : '--',
      tone: 'from-emerald-500/15 to-teal-500/10',
    },
    {
      label: '最新服务响应',
      value: latestService?.responseTimeMs != null ? `${latestService.responseTimeMs} ms` : '--',
      tone: 'from-amber-500/15 to-orange-500/10',
    },
    {
      label: '异常检测',
      value: latestService?.errorCount ?? '--',
      tone: 'from-rose-500/15 to-fuchsia-500/10',
    },
  ];
});

const trendCards = computed(() => {
  return [
    {
      color: '#06b6d4',
      label: 'CPU 波动',
      suffix: '%',
      values: filteredServerRows.value
        .slice(0, 8)
        .map((item) => item.cpuUsage)
        .filter((item) => item != null),
    },
    {
      color: '#10b981',
      label: '内存波动',
      suffix: '%',
      values: filteredServerRows.value
        .slice(0, 8)
        .map((item) => item.memoryUsage)
        .filter((item) => item != null),
    },
    {
      color: '#f59e0b',
      label: '服务响应',
      suffix: 'ms',
      values: filteredServiceRows.value
        .slice(0, 8)
        .map((item) => item.responseTimeMs)
        .filter((item) => item != null),
    },
    {
      color: '#ef4444',
      label: '错误次数',
      suffix: '',
      values: filteredServiceRows.value
        .slice(0, 8)
        .map((item) => item.errorCount)
        .filter((item) => item != null),
    },
  ];
});

function formatPercent(value?: number) {
  return value == null ? '--' : `${value}%`;
}

function formatBytes(value?: number) {
  if (value == null) return '--';
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  if (value < 1024 * 1024 * 1024) return `${(value / 1024 / 1024).toFixed(1)} MB`;
  return `${(value / 1024 / 1024 / 1024).toFixed(2)} GB`;
}

function getLastValue(values: number[]) {
  if (values.length === 0) return '--';
  return values[values.length - 1];
}

function buildSparklinePath(values: number[]) {
  if (values.length === 0) return '';
  if (values.length === 1) return 'M 0 24 L 100 24';
  const min = Math.min(...values);
  const max = Math.max(...values);
  return values
    .map((value, index) => {
      const x = (index / (values.length - 1)) * 100;
      const y =
        max === min
          ? 24
          : 44 - ((value - min) / (max - min)) * 40;
      return `${index === 0 ? 'M' : 'L'} ${x.toFixed(2)} ${y.toFixed(2)}`;
    })
    .join(' ');
}

function serviceStatusLabel(status?: number) {
  if (status === 1) return { color: 'success', text: 'healthy' };
  if (status === 0) return { color: 'default', text: 'unknown' };
  if (status != null && status < 0) return { color: 'error', text: 'error' };
  return { color: 'processing', text: `${status ?? '--'}` };
}

async function loadAll() {
  loading.value = true;
  try {
    const [serverData, serviceData] = await Promise.all([
      getServerMetricList(serverQuery),
      getServiceMetricList(serviceQuery),
    ]);
    serverRows.value = serverData.list;
    serverTotal.value = serverData.total;
    serviceRows.value = serviceData.list;
    serviceTotal.value = serviceData.total;
  } finally {
    loading.value = false;
  }
}

let reloadTimer: null | ReturnType<typeof setTimeout> = null;
function scheduleReload() {
  if (reloadTimer) {
    clearTimeout(reloadTimer);
  }
  reloadTimer = setTimeout(() => {
    loadAll();
    reloadTimer = null;
  }, 500);
}

const { connectionLabel, lastMessage } = useNetbridgeRealtime({
  onServerStatus: scheduleReload,
  onServiceStatus: scheduleReload,
});

onMounted(loadAll);
</script>

<template>
  <div class="p-5">
    <div class="mb-5 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <Card
        v-for="item in summary"
        :key="item.label"
        class="overflow-hidden rounded-2xl border-0 shadow-sm"
      >
        <div :class="['rounded-2xl border border-slate-200/60 bg-gradient-to-br p-5', item.tone]">
          <div class="text-xs uppercase tracking-[0.22em] text-slate-500">{{ item.label }}</div>
          <div class="mt-3 text-3xl font-semibold text-slate-900">{{ item.value }}</div>
        </div>
      </Card>
    </div>

    <div class="mb-5 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <Card
        v-for="card in trendCards"
        :key="card.label"
        class="rounded-2xl border-0 shadow-sm"
      >
        <div class="text-xs uppercase tracking-[0.18em] text-slate-500">{{ card.label }}</div>
        <div class="mt-3 flex items-end justify-between">
          <div class="text-3xl font-semibold text-slate-900">
            {{ getLastValue(card.values as number[]) }}
            <span class="ml-1 text-sm font-medium text-slate-400">{{ card.suffix }}</span>
          </div>
          <div class="text-xs text-slate-400">latest</div>
        </div>
        <div class="mt-4 h-14 rounded-2xl bg-slate-50 px-2 py-1">
          <svg class="h-full w-full" preserveAspectRatio="none" viewBox="0 0 100 48">
            <path
              :d="buildSparklinePath(card.values as number[])"
              :stroke="card.color"
              fill="none"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="3"
            />
          </svg>
        </div>
      </Card>
    </div>

    <Card class="rounded-2xl border-0 shadow-sm">
      <div class="mb-4">
        <div class="flex flex-col gap-2 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 class="text-xl font-semibold text-slate-900">监控中心</h2>
            <p class="mt-1 text-sm text-slate-500">按服务器与服务维度查看最新采集指标，先完成稳定联调，再逐步演进成趋势图。</p>
          </div>
          <Space>
            <Select
              v-model:value="timeRangeHours"
              style="width: 160px"
              :options="[
                { label: '最近 1 小时', value: 1 },
                { label: '最近 6 小时', value: 6 },
                { label: '最近 24 小时', value: 24 },
                { label: '最近 72 小时', value: 72 },
              ]"
            />
            <Tag :color="connectionLabel === 'live' ? 'success' : connectionLabel === 'connecting' ? 'processing' : 'default'">
              {{ connectionLabel }}
            </Tag>
            <Tag v-if="lastMessage?.message" color="cyan">{{ lastMessage.message }}</Tag>
          </Space>
        </div>
      </div>

      <Tabs v-model:activeKey="activeKey">
        <Tabs.TabPane key="server" tab="服务器指标">
          <Space class="mb-4" wrap>
            <InputNumber v-model:value="serverQuery.serverId" placeholder="服务器 ID" style="width: 180px" />
            <Button type="primary" @click="loadAll">查询</Button>
          </Space>

          <Table
            :columns="serverColumns"
            :data-source="filteredServerRows"
            :loading="loading"
            :pagination="{
              current: serverQuery.pageNo,
              pageSize: serverQuery.pageSize,
              total: serverTotal,
              onChange: (page: number, pageSize: number) => {
                serverQuery.pageNo = page;
                serverQuery.pageSize = pageSize;
                loadAll();
              },
            }"
            :row-key="(record: any) => record.id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'cpuUsage'">
                <span class="font-medium text-slate-900">{{ formatPercent(record.cpuUsage) }}</span>
              </template>
              <template v-else-if="column.key === 'memoryUsage'">
                <span class="font-medium text-slate-900">{{ formatPercent(record.memoryUsage) }}</span>
              </template>
              <template v-else-if="column.key === 'diskUsage'">
                <span class="font-medium text-slate-900">{{ formatPercent(record.diskUsage) }}</span>
              </template>
              <template v-else-if="column.key === 'networkInBytes'">
                {{ formatBytes(record.networkInBytes) }}
              </template>
              <template v-else-if="column.key === 'networkOutBytes'">
                {{ formatBytes(record.networkOutBytes) }}
              </template>
            </template>
          </Table>
        </Tabs.TabPane>

        <Tabs.TabPane key="service" tab="服务指标">
          <Space class="mb-4" wrap>
            <InputNumber v-model:value="serviceQuery.serverId" placeholder="服务器 ID" style="width: 180px" />
            <InputNumber v-model:value="serviceQuery.serviceId" placeholder="服务 ID" style="width: 180px" />
            <Button type="primary" @click="loadAll">查询</Button>
          </Space>

          <Table
            :columns="serviceColumns"
            :data-source="filteredServiceRows"
            :loading="loading"
            :pagination="{
              current: serviceQuery.pageNo,
              pageSize: serviceQuery.pageSize,
              total: serviceTotal,
              onChange: (page: number, pageSize: number) => {
                serviceQuery.pageNo = page;
                serviceQuery.pageSize = pageSize;
                loadAll();
              },
            }"
            :row-key="(record: any) => record.id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <Tag :color="serviceStatusLabel(record.status).color">
                  {{ serviceStatusLabel(record.status).text }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'responseTimeMs'">
                {{ record.responseTimeMs != null ? `${record.responseTimeMs} ms` : '--' }}
              </template>
            </template>
          </Table>
        </Tabs.TabPane>
      </Tabs>
    </Card>
  </div>
</template>
