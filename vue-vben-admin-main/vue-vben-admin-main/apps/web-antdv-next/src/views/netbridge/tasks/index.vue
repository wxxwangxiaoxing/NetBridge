<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';

import { Button, Card, Descriptions, Divider, Drawer, Empty, Popconfirm, Select, Space, Table, Tag, TypographyParagraph, message } from 'antdv-next';

import { useNetbridgeRealtime } from '#/api/netbridge/realtime';
import { cancelTask, getTaskList, retryTask } from '#/api/netbridge/task';

const loading = ref(false);
const rows = ref<any[]>([]);
const total = ref(0);
const detailOpen = ref(false);
const currentTask = ref<any | null>(null);
const detailMode = ref<'raw' | 'structured'>('structured');
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  status: undefined as string | undefined,
  type: undefined as string | undefined,
});

const columns = [
  { dataIndex: 'id', key: 'id', title: '任务ID', width: 100 },
  { dataIndex: 'type', key: 'type', title: '类型', width: 120 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 120 },
  { dataIndex: 'serverId', key: 'serverId', title: '服务器ID', width: 120 },
  { dataIndex: 'serviceId', key: 'serviceId', title: '服务ID', width: 120 },
  { dataIndex: 'createdAt', key: 'createdAt', title: '创建时间' },
  { key: 'actions', title: '操作', width: 220 },
];

function statusColor(status?: string) {
  switch (status) {
    case 'success':
      return 'success';
    case 'running':
      return 'processing';
    case 'failed':
    case 'error':
    case 'timed_out':
      return 'error';
    case 'cancelled':
      return 'warning';
    default:
      return 'default';
  }
}

function actionTone(status?: string) {
  switch (status) {
    case 'active':
    case 'connected':
    case 'configured':
    case 'healthy':
    case 'open':
    case 'removed':
    case 'success':
      return 'success';
    case 'failed':
    case 'error':
    case 'inactive':
      return 'error';
    case 'skipped':
    case 'unknown':
      return 'default';
    default:
      return 'processing';
  }
}

const payloadSummary = computed(() => currentTask.value?.payloadObj || {});
const resultSummary = computed(() => currentTask.value?.resultObj || null);

const resultOverview = computed(() => {
  const result = resultSummary.value;
  if (!result) return [];
  return [
    { label: '执行结果', value: result.success ? 'success' : 'failed', tone: result.success ? 'success' : 'error' },
    { label: '服务状态', value: result.serviceStatus || '--', tone: actionTone(result.serviceStatus) },
    { label: '端口连通', value: result.portStatus || '--', tone: actionTone(result.portStatus) },
    { label: 'Tailscale', value: result.tailscaleStatus || '--', tone: actionTone(result.tailscaleStatus) },
    { label: 'Tunnel', value: result.tunnelStatus || '--', tone: actionTone(result.tunnelStatus) },
    { label: '响应时间', value: result.responseTimeMs != null ? `${result.responseTimeMs} ms` : '--', tone: 'processing' },
  ];
});

const actionSections = computed(() => {
  const result = resultSummary.value;
  if (!result) return [];
  return [
    { key: 'serviceAction', title: '服务动作', value: result.serviceAction },
    { key: 'tailscaleAction', title: 'Tailscale 动作', value: result.tailscaleAction },
    { key: 'tunnelAction', title: 'Tunnel 动作', value: result.tunnelAction },
  ].filter((item) => item.value);
});

const failureSignals = computed(() => {
  const task = currentTask.value;
  const result = resultSummary.value;
  if (!task) return [];
  const signals = [
    {
      label: '任务错误',
      value: task.errorMessage,
    },
    {
      label: '执行消息',
      value: result?.message,
    },
    {
      label: '服务动作',
      value:
        result?.serviceAction?.status === 'failed'
          ? result?.serviceAction?.detail || result?.serviceAction?.status
          : null,
    },
    {
      label: 'Tailscale 动作',
      value:
        result?.tailscaleAction?.status === 'failed'
          ? result?.tailscaleAction?.detail || result?.tailscaleAction?.status
          : null,
    },
    {
      label: 'Tunnel 动作',
      value:
        result?.tunnelAction?.status === 'failed'
          ? result?.tunnelAction?.detail || result?.tunnelAction?.status
          : null,
    },
    {
      label: '端口状态',
      value:
        result?.portStatus && result?.portStatus !== 'open'
          ? `port status: ${result.portStatus}`
          : null,
    },
  ].filter((item) => item.value);
  return signals;
});

const failureHeadline = computed(() => {
  if (!currentTask.value) return null;
  if (currentTask.value.status !== 'failed' && currentTask.value.status !== 'timed_out') {
    return null;
  }
  return failureSignals.value[0]?.value || '当前任务失败，但还没有更详细的结构化原因。';
});

const { connectionLabel, lastMessage } = useNetbridgeRealtime({
  onTaskStatus(payload) {
    const target = rows.value.find((item) => item.id === payload.taskId || item.id === payload.entityId);
    if (target) {
      target.status = payload.status || target.status;
    }
    if (currentTask.value && (currentTask.value.id === payload.taskId || currentTask.value.id === payload.entityId)) {
      currentTask.value = {
        ...currentTask.value,
        status: payload.status || currentTask.value.status,
      };
    }
  },
});

async function handleRetry(id: number) {
  await retryTask(id);
  message.success('任务已重新进入 pending');
  await loadData();
}

async function handleCancel(id: number) {
  await cancelTask(id);
  message.success('任务已取消');
  await loadData();
}

function openDetail(record: any) {
  currentTask.value = record;
  detailMode.value = 'structured';
  detailOpen.value = true;
}

async function loadData() {
  loading.value = true;
  try {
    const data = await getTaskList(query);
    rows.value = data.list;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
</script>

<template>
  <div class="p-5">
    <Card class="rounded-2xl border-0 shadow-sm">
      <div class="mb-4">
        <div class="flex flex-col gap-2 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 class="text-xl font-semibold text-slate-900">任务中心</h2>
            <p class="mt-1 text-sm text-slate-500">观察 pending / running / success / failed 的完整执行链。</p>
          </div>
          <Space>
            <Tag :color="connectionLabel === 'live' ? 'success' : connectionLabel === 'connecting' ? 'processing' : 'default'">
              {{ connectionLabel }}
            </Tag>
            <Tag v-if="lastMessage?.message" color="blue">{{ lastMessage.message }}</Tag>
          </Space>
        </div>
      </div>

      <Space class="mb-4" wrap>
        <Select
          v-model:value="query.type"
          allow-clear
          placeholder="任务类型"
          style="width: 180px"
          :options="[
            { label: 'START', value: 'START' },
            { label: 'STOP', value: 'STOP' },
            { label: 'RESTART', value: 'RESTART' },
            { label: 'CHECK', value: 'CHECK' },
          ]"
        />
        <Select
          v-model:value="query.status"
          allow-clear
          placeholder="任务状态"
          style="width: 180px"
          :options="[
            { label: 'pending', value: 'pending' },
            { label: 'running', value: 'running' },
            { label: 'success', value: 'success' },
            { label: 'failed', value: 'failed' },
            { label: 'cancelled', value: 'cancelled' },
            { label: 'timed_out', value: 'timed_out' },
          ]"
        />
        <Button type="primary" @click="loadData">查询</Button>
      </Space>

      <Table
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="{
          current: query.pageNo,
          pageSize: query.pageSize,
          total,
          onChange: (page: number, pageSize: number) => {
            query.pageNo = page;
            query.pageSize = pageSize;
            loadData();
          },
        }"
        :row-key="(record: any) => record.id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <Tag :color="statusColor(record.status)">{{ record.status }}</Tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <Space>
              <Button size="small" @click="openDetail(record)">详情</Button>
              <Button
                v-if="record.status === 'failed' || record.status === 'timed_out'"
                size="small"
                type="primary"
                ghost
                @click="handleRetry(record.id)"
              >
                重试
              </Button>
              <Popconfirm
                v-if="record.status === 'pending' || record.status === 'running'"
                title="确定取消该任务？"
                @confirm="handleCancel(record.id)"
              >
                <Button danger size="small">取消</Button>
              </Popconfirm>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <Drawer v-model:open="detailOpen" title="任务详情" width="720">
      <template v-if="currentTask">
        <div class="space-y-4">
          <div class="flex justify-end">
            <Select
              v-model:value="detailMode"
              style="width: 180px"
              :options="[
                { label: '结构化视图', value: 'structured' },
                { label: '原始 JSON', value: 'raw' },
              ]"
            />
          </div>
          <Card size="small" title="基础信息">
            <div class="grid grid-cols-2 gap-3 text-sm text-slate-600">
              <div>任务ID：{{ currentTask.id }}</div>
              <div>类型：{{ currentTask.type }}</div>
              <div>状态：{{ currentTask.status }}</div>
              <div>服务ID：{{ currentTask.serviceId || '-' }}</div>
            </div>
          </Card>
          <template v-if="detailMode === 'structured'">
            <Card v-if="resultSummary" size="small" title="结果总览">
            <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
              <div
                v-for="item in resultOverview"
                :key="item.label"
                class="rounded-2xl border border-slate-200 bg-slate-50 p-4"
              >
                <div class="text-xs uppercase tracking-[0.18em] text-slate-500">{{ item.label }}</div>
                <div class="mt-3 flex items-center gap-2">
                  <Tag :color="item.tone">{{ item.value }}</Tag>
                </div>
              </div>
            </div>
            <Divider />
            <Descriptions :column="1" bordered size="small">
              <Descriptions.Item label="访问地址">
                <div class="break-all text-slate-700">
                  {{ resultSummary.accessUrl || resultSummary.endpoint || '--' }}
                </div>
              </Descriptions.Item>
              <Descriptions.Item label="执行消息">
                {{ resultSummary.message || currentTask.errorMessage || '--' }}
              </Descriptions.Item>
              <Descriptions.Item label="回滚补偿">
                <Tag :color="resultSummary.rollbackApplied ? 'warning' : 'default'">
                  {{ resultSummary.rollbackApplied ? '已执行' : '未执行' }}
                </Tag>
              </Descriptions.Item>
            </Descriptions>
            </Card>
            <Card
              v-if="failureHeadline"
              size="small"
              title="故障判断"
            >
              <div class="rounded-2xl border border-red-200 bg-red-50 p-4">
                <div class="text-xs uppercase tracking-[0.18em] text-red-500">Primary Signal</div>
                <div class="mt-3 text-sm font-medium leading-6 text-red-700">
                  {{ failureHeadline }}
                </div>
              </div>
              <div class="mt-4 grid gap-3">
                <div
                  v-for="item in failureSignals"
                  :key="item.label"
                  class="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3"
                >
                  <div class="text-xs uppercase tracking-[0.16em] text-slate-500">{{ item.label }}</div>
                  <div class="mt-2 text-sm leading-6 text-slate-700">{{ item.value }}</div>
                </div>
              </div>
            </Card>
            <Card size="small" title="任务载荷">
              <Descriptions :column="2" bordered size="small">
                <Descriptions.Item label="动作">{{ payloadSummary.action || currentTask.type || '--' }}</Descriptions.Item>
                <Descriptions.Item label="服务名">{{ payloadSummary.serviceName || '--' }}</Descriptions.Item>
                <Descriptions.Item label="协议">{{ payloadSummary.protocol || '--' }}</Descriptions.Item>
                <Descriptions.Item label="端口">{{ payloadSummary.port ?? '--' }}</Descriptions.Item>
                <Descriptions.Item label="接入方式">{{ payloadSummary.accessType || '--' }}</Descriptions.Item>
                <Descriptions.Item label="目标地址">{{ payloadSummary.targetHost || '--' }}</Descriptions.Item>
                <Descriptions.Item label="域名" :span="2">{{ payloadSummary.domain || '--' }}</Descriptions.Item>
              </Descriptions>
            </Card>
            <Card v-if="actionSections.length > 0" size="small" title="执行链动作">
              <div class="space-y-4">
                <div
                  v-for="section in actionSections"
                  :key="section.key"
                  class="rounded-2xl border border-slate-200 bg-slate-50 p-4"
                >
                  <div class="mb-3 text-sm font-medium text-slate-900">{{ section.title }}</div>
                  <Descriptions :column="2" bordered size="small">
                    <Descriptions.Item label="组件">{{ section.value.component || '--' }}</Descriptions.Item>
                    <Descriptions.Item label="动作">{{ section.value.action || '--' }}</Descriptions.Item>
                    <Descriptions.Item label="状态">
                      <Tag :color="actionTone(section.value.status)">
                        {{ section.value.status || '--' }}
                      </Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="模式">{{ section.value.mode || '--' }}</Descriptions.Item>
                    <Descriptions.Item label="详情" :span="2">
                      {{ section.value.detail || '--' }}
                    </Descriptions.Item>
                  </Descriptions>
                </div>
              </div>
            </Card>
          </template>
          <template v-else>
            <Card size="small" title="PayloadObj">
              <TypographyParagraph>
                <pre class="whitespace-pre-wrap text-xs">{{ JSON.stringify(currentTask.payloadObj, null, 2) }}</pre>
              </TypographyParagraph>
            </Card>
            <Card size="small" title="ResultObj">
              <template v-if="currentTask.resultObj">
                <TypographyParagraph>
                  <pre class="whitespace-pre-wrap text-xs">{{ JSON.stringify(currentTask.resultObj, null, 2) }}</pre>
                </TypographyParagraph>
              </template>
              <Empty v-else description="暂无结果" />
            </Card>
          </template>
        </div>
      </template>
    </Drawer>
  </div>
</template>
