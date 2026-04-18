<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { Button, Card, Form, Input, Modal, Select, Space, Table, Tag, message } from 'antdv-next';

import { useNetbridgeRealtime } from '#/api/netbridge/realtime';
import { generateInstallCommand, getServerList } from '#/api/netbridge/server';

const router = useRouter();
const loading = ref(false);
const rows = ref<any[]>([]);
const total = ref(0);
const installModalOpen = ref(false);
const installLoading = ref(false);
const installResult = ref<any | null>(null);
const installForm = reactive({
  hostname: '',
});
const query = reactive({
  hostname: '',
  pageNo: 1,
  pageSize: 10,
  status: undefined as string | undefined,
});

const columns = [
  { dataIndex: 'hostname', key: 'hostname', title: '主机名' },
  { dataIndex: 'displayName', key: 'displayName', title: '显示名' },
  { dataIndex: 'ip', key: 'ip', title: 'IP' },
  { dataIndex: 'tailscaleIp', key: 'tailscaleIp', title: 'Tailscale IP' },
  { dataIndex: 'status', key: 'status', title: '状态' },
  { dataIndex: 'lastHeartbeat', key: 'lastHeartbeat', title: '最后心跳' },
];

function statusColor(status?: string) {
  switch (status) {
    case 'online':
      return 'success';
    case 'error':
      return 'error';
    default:
      return 'default';
  }
}

const { connectionLabel } = useNetbridgeRealtime({
  onServerStatus(payload) {
    const target = rows.value.find((item) => item.id === payload.serverId || item.id === payload.entityId);
    if (!target) return;
    target.status = payload.status || target.status;
    target.lastHeartbeat = payload.timestamp || target.lastHeartbeat;
  },
});

async function loadData() {
  loading.value = true;
  try {
    const data = await getServerList(query);
    rows.value = data.list;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
}

async function openInstallModal() {
  installForm.hostname = '';
  installResult.value = null;
  installModalOpen.value = true;
}

async function submitInstallCommand() {
  installLoading.value = true;
  try {
    installResult.value = await generateInstallCommand(installForm.hostname || undefined);
    message.success('安装命令已生成');
  } finally {
    installLoading.value = false;
  }
}

onMounted(loadData);
</script>

<template>
  <div class="p-5">
    <Card class="rounded-2xl border-0 shadow-sm">
      <div class="mb-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h2 class="text-xl font-semibold text-slate-900">服务器管理</h2>
          <p class="mt-1 text-sm text-slate-500">集中查看 Agent 注册节点、在线状态与安装入口。</p>
        </div>
        <Space>
          <Tag :color="connectionLabel === 'live' ? 'success' : connectionLabel === 'connecting' ? 'processing' : 'default'">
            {{ connectionLabel }}
          </Tag>
          <Button type="primary" @click="openInstallModal">生成安装命令</Button>
        </Space>
      </div>

      <Space class="mb-4" wrap>
        <Input v-model:value="query.hostname" allow-clear placeholder="按主机名筛选" style="width: 220px" />
        <Select
          v-model:value="query.status"
          allow-clear
          placeholder="状态"
          style="width: 160px"
          :options="[
            { label: '在线', value: 'online' },
            { label: '离线', value: 'offline' },
            { label: '异常', value: 'error' },
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
          <template v-if="column.key === 'hostname'">
            <button class="font-medium text-left text-sky-600 hover:text-sky-500" @click="router.push(`/netbridge/servers/${record.id}`)">
              {{ record.hostname }}
            </button>
            <div class="text-xs text-slate-500">{{ record.os || 'unknown-os' }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="statusColor(record.status)">{{ record.status }}</Tag>
          </template>
        </template>
      </Table>
    </Card>

    <Modal v-model:open="installModalOpen" title="生成安装命令" :confirm-loading="installLoading" @ok="submitInstallCommand">
      <Form layout="vertical">
        <Form.Item label="预期主机名">
          <Input v-model:value="installForm.hostname" placeholder="可选，便于安装时预填识别" />
        </Form.Item>
      </Form>
      <div v-if="installResult" class="rounded-2xl bg-slate-950 p-4 text-slate-100">
        <div class="text-xs uppercase tracking-[0.25em] text-emerald-300">Install Command</div>
        <pre class="mt-3 overflow-auto whitespace-pre-wrap text-xs leading-6">{{ installResult.command }}</pre>
        <div class="mt-3 text-xs text-slate-400">token: {{ installResult.token }}</div>
        <div class="mt-1 text-xs text-slate-400">expiresAt: {{ installResult.expiresAt }}</div>
      </div>
    </Modal>
  </div>
</template>
