<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';

import { Button, Card, Descriptions, Drawer, Input, Popconfirm, Select, Space, Table, Tag, message } from 'antdv-next';

import { getInstallTokenList, revokeInstallToken } from '#/api/netbridge/server';

const loading = ref(false);
const rows = ref<any[]>([]);
const total = ref(0);
const detailOpen = ref(false);
const currentToken = ref<any | null>(null);
const query = reactive({
  hostname: '',
  pageNo: 1,
  pageSize: 10,
  status: undefined as string | undefined,
});

const columns = [
  { dataIndex: 'token', key: 'token', title: '令牌' },
  { dataIndex: 'hostname', key: 'hostname', title: '主机名' },
  { dataIndex: 'status', key: 'status', title: '状态' },
  { dataIndex: 'expiresAt', key: 'expiresAt', title: '过期时间' },
  { dataIndex: 'usedAt', key: 'usedAt', title: '使用时间' },
  { dataIndex: 'createdAt', key: 'createdAt', title: '创建时间' },
  { dataIndex: 'actions', key: 'actions', title: '操作', width: 180 },
];

const summary = computed(() => {
  const counts = rows.value.reduce(
    (acc, item) => {
      acc[item.status] = (acc[item.status] || 0) + 1;
      return acc;
    },
    {} as Record<string, number>,
  );
  return [
    { label: '未使用', value: counts.unused || 0, color: 'blue' },
    { label: '已使用', value: counts.used || 0, color: 'success' },
    { label: '已失效', value: counts.revoked || 0, color: 'default' },
    { label: '已过期', value: counts.expired || 0, color: 'error' },
  ];
});

function tokenColor(status?: string) {
  switch (status) {
    case 'unused':
      return 'blue';
    case 'used':
      return 'success';
    case 'revoked':
      return 'default';
    case 'expired':
      return 'error';
    default:
      return 'default';
  }
}

function openDetail(record: any) {
  currentToken.value = record;
  detailOpen.value = true;
}

async function copyToken(token?: string) {
  if (!token) return;
  await navigator.clipboard.writeText(token);
  message.success('令牌已复制');
}

async function loadData() {
  loading.value = true;
  try {
    const data = await getInstallTokenList(query);
    rows.value = data.list;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
}

async function handleRevoke(id: number) {
  await revokeInstallToken(id);
  message.success('安装令牌已失效');
  await loadData();
}

onMounted(loadData);
</script>

<template>
  <div class="p-5">
    <div class="mb-5 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <Card
        v-for="item in summary"
        :key="item.label"
        class="rounded-2xl border-0 shadow-sm"
      >
        <div class="text-xs uppercase tracking-[0.18em] text-slate-500">{{ item.label }}</div>
        <div class="mt-3">
          <Tag :color="item.color">{{ item.value }}</Tag>
        </div>
      </Card>
    </div>

    <Card class="rounded-2xl border-0 shadow-sm">
      <div class="mb-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h2 class="text-xl font-semibold text-slate-900">安装令牌</h2>
          <p class="mt-1 text-sm text-slate-500">查看安装令牌发放、使用与失效状态，收紧 Agent 接入入口。</p>
        </div>
      </div>

      <Space class="mb-4" wrap>
        <Input v-model:value="query.hostname" allow-clear placeholder="按主机名筛选" style="width: 220px" />
        <Select
          v-model:value="query.status"
          allow-clear
          placeholder="状态"
          style="width: 160px"
          :options="[
            { label: '未使用', value: 'unused' },
            { label: '已使用', value: 'used' },
            { label: '已失效', value: 'revoked' },
            { label: '已过期', value: 'expired' },
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
          <template v-if="column.key === 'token'">
            <div class="font-medium text-slate-900">{{ record.token }}</div>
            <div class="text-xs text-slate-500">ID: {{ record.id }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="tokenColor(record.status)">{{ record.status }}</Tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <Space size="small">
              <Button size="small" type="link" @click="openDetail(record)">详情</Button>
              <Button size="small" type="link" @click="copyToken(record.token)">复制</Button>
              <Popconfirm title="确认让这个安装令牌失效吗？" @confirm="handleRevoke(record.id)">
                <Button size="small" type="link" :disabled="record.status !== 'unused'">失效</Button>
              </Popconfirm>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <Drawer v-model:open="detailOpen" title="安装令牌详情" width="640">
      <template v-if="currentToken">
        <div class="space-y-4">
          <Card size="small" title="状态概览">
            <div class="grid grid-cols-2 gap-3 text-sm text-slate-600">
              <div>主机名：{{ currentToken.hostname || '--' }}</div>
              <div>
                状态：
                <Tag :color="tokenColor(currentToken.status)">{{ currentToken.status }}</Tag>
              </div>
              <div>创建时间：{{ currentToken.createdAt || '--' }}</div>
              <div>过期时间：{{ currentToken.expiresAt || '--' }}</div>
            </div>
          </Card>

          <Card size="small" title="令牌内容">
            <Descriptions :column="1" bordered size="small">
              <Descriptions.Item label="Token ID">
                {{ currentToken.id }}
              </Descriptions.Item>
              <Descriptions.Item label="Token">
                <div class="break-all font-mono text-xs text-slate-700">
                  {{ currentToken.token }}
                </div>
              </Descriptions.Item>
              <Descriptions.Item label="使用时间">
                {{ currentToken.usedAt || '--' }}
              </Descriptions.Item>
            </Descriptions>
            <div class="mt-4">
              <Button type="primary" @click="copyToken(currentToken.token)">复制令牌</Button>
            </div>
          </Card>
        </div>
      </template>
    </Drawer>
  </div>
</template>
