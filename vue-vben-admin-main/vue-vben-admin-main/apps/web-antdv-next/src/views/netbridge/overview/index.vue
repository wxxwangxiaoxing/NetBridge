<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue';

import { Alert, Card, Col, Empty, Row, Skeleton, Space, Statistic, Table, Tag } from 'antdv-next';

import { useNetbridgeRealtime } from '#/api/netbridge/realtime';
import { getOverviewData } from '#/api/netbridge/overview';

const loading = ref(true);
const summary = ref({
  onlineServers: 0,
  runningTasks: 0,
  servers: 0,
  services: 0,
});
const recentTasks = ref<any[]>([]);
const failedTasks = ref<any[]>([]);
const urgentLogs = ref<any[]>([]);

const taskColumns = [
  { dataIndex: 'id', key: 'id', title: '任务ID', width: 100 },
  { dataIndex: 'type', key: 'type', title: '类型', width: 120 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 120 },
  { dataIndex: 'serviceId', key: 'serviceId', title: '服务ID', width: 120 },
  { dataIndex: 'createdAt', key: 'createdAt', title: '创建时间' },
];

const logColumns = [
  { dataIndex: 'level', key: 'level', title: '级别', width: 100 },
  { dataIndex: 'source', key: 'source', title: '来源', width: 120 },
  { dataIndex: 'content', key: 'content', title: '内容' },
  { dataIndex: 'createdAt', key: 'createdAt', title: '时间', width: 180 },
];

const headline = computed(() => {
  return `${summary.value.onlineServers}/${summary.value.servers} 台服务器在线，${summary.value.runningTasks} 条任务正在执行`;
});

function statusColor(status?: string) {
  switch (status) {
    case 'success':
      return 'success';
    case 'failed':
    case 'error':
      return 'error';
    case 'running':
      return 'processing';
    default:
      return 'default';
  }
}

async function loadData() {
  loading.value = true;
  try {
    const data = await getOverviewData();
    summary.value = data.summary;
    recentTasks.value = data.recentTasks;
    failedTasks.value = data.failedTasks;
    urgentLogs.value = data.urgentLogs;
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
    loadData();
    reloadTimer = null;
  }, 400);
}

const { connectionLabel, lastMessage } = useNetbridgeRealtime({
  onServerStatus: scheduleReload,
  onServiceStatus: scheduleReload,
  onTaskStatus: scheduleReload,
});

onMounted(loadData);
</script>

<template>
  <div class="min-h-full bg-[radial-gradient(circle_at_top_left,_rgba(20,184,166,0.18),_transparent_28%),radial-gradient(circle_at_top_right,_rgba(249,115,22,0.14),_transparent_24%),linear-gradient(180deg,_#07111f_0%,_#0b172a_55%,_#f4f7fb_55%,_#f4f7fb_100%)] p-6">
    <div class="mb-6 overflow-hidden rounded-3xl border border-white/10 bg-[#08101d]/80 p-6 text-white shadow-[0_24px_80px_rgba(2,6,23,0.45)] backdrop-blur">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div class="max-w-3xl">
          <div class="mb-2 inline-flex items-center rounded-full border border-emerald-400/30 bg-emerald-400/10 px-3 py-1 text-xs uppercase tracking-[0.3em] text-emerald-200">
            NetBridge Mission Control
          </div>
          <h1 class="text-3xl font-semibold tracking-tight text-white lg:text-4xl">
            零信任隧道、Agent 编排和服务状态，在一张控制台里完成闭环。
          </h1>
          <p class="mt-3 text-sm leading-6 text-slate-300 lg:text-base">
            {{ headline }}
          </p>
          <div class="mt-4 flex flex-wrap items-center gap-2">
            <Tag :color="connectionLabel === 'live' ? 'success' : connectionLabel === 'connecting' ? 'processing' : 'default'">
              {{ connectionLabel }}
            </Tag>
            <Tag v-if="lastMessage?.message" color="cyan">{{ lastMessage.message }}</Tag>
          </div>
        </div>
        <Alert
          class="max-w-xl border-orange-400/30 bg-orange-500/10 text-orange-50"
          message="当前建议先优先观察 failed 任务和 ERROR 日志，再处理 agent 心跳缺失的节点。"
          show-icon
          type="warning"
        />
      </div>
    </div>

    <Skeleton :loading="loading" active>
      <Row :gutter="[16, 16]">
        <Col :lg="6" :md="12" :span="24">
          <Card class="rounded-2xl border-0 shadow-sm">
            <Statistic title="服务器总数" :value="summary.servers" />
          </Card>
        </Col>
        <Col :lg="6" :md="12" :span="24">
          <Card class="rounded-2xl border-0 shadow-sm">
            <Statistic title="在线服务器" :value="summary.onlineServers" />
          </Card>
        </Col>
        <Col :lg="6" :md="12" :span="24">
          <Card class="rounded-2xl border-0 shadow-sm">
            <Statistic title="服务总数" :value="summary.services" />
          </Card>
        </Col>
        <Col :lg="6" :md="12" :span="24">
          <Card class="rounded-2xl border-0 shadow-sm">
            <Statistic title="运行中任务" :value="summary.runningTasks" />
          </Card>
        </Col>
      </Row>

      <Row class="mt-4" :gutter="[16, 16]">
        <Col :lg="16" :span="24">
          <Card class="rounded-2xl border-0 shadow-sm" title="最近任务">
            <Table :columns="taskColumns" :data-source="recentTasks" :pagination="false" :row-key="(record: any) => record.id">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <Tag :color="statusColor(record.status)">{{ record.status }}</Tag>
                </template>
              </template>
            </Table>
          </Card>
        </Col>
        <Col :lg="8" :span="24">
          <Card class="rounded-2xl border-0 shadow-sm" title="失败任务">
            <Space direction="vertical" class="w-full">
              <template v-if="failedTasks.length > 0">
                <div
                  v-for="task in failedTasks"
                  :key="task.id"
                  class="rounded-xl border border-red-100 bg-red-50 p-3"
                >
                  <div class="flex items-center justify-between">
                    <span class="font-medium text-slate-900">#{{ task.id }} {{ task.type }}</span>
                    <Tag color="error">{{ task.status }}</Tag>
                  </div>
                  <div class="mt-2 text-xs text-slate-500">
                    serviceId: {{ task.serviceId || '-' }}
                  </div>
                  <div class="mt-2 text-xs text-slate-500">
                    {{ task.errorMessage || '等待查看详情页中的 resultObj / errorMessage' }}
                  </div>
                </div>
              </template>
              <Empty v-else description="暂无失败任务" />
            </Space>
          </Card>
        </Col>
      </Row>

      <Card class="mt-4 rounded-2xl border-0 shadow-sm" title="紧急日志">
        <Table :columns="logColumns" :data-source="urgentLogs" :pagination="false" :row-key="(record: any) => record.id">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'level'">
              <Tag color="error">{{ record.level }}</Tag>
            </template>
          </template>
        </Table>
      </Card>
    </Skeleton>
  </div>
</template>
