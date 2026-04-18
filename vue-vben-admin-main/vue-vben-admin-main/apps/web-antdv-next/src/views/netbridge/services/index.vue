<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, Tag, message } from 'antdv-next';

import { useNetbridgeRealtime } from '#/api/netbridge/realtime';
import {
  checkService,
  createService,
  deleteService,
  getServiceList,
  restartService,
  startService,
  stopService,
  updateService,
} from '#/api/netbridge/service';

const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const modalOpen = ref(false);
const editingId = ref<number | null>(null);
const rows = ref<any[]>([]);
const total = ref(0);
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  status: undefined as string | undefined,
  type: '',
});
const form = reactive({
  accessType: 'tailscale',
  domain: '',
  name: '',
  port: 80,
  protocol: 'HTTP',
  serverId: 1,
  targetHost: '127.0.0.1',
  type: 'Web',
});

const columns = [
  { dataIndex: 'name', key: 'name', title: '服务名' },
  { dataIndex: 'type', key: 'type', title: '类型' },
  { dataIndex: 'protocol', key: 'protocol', title: '协议' },
  { dataIndex: 'port', key: 'port', title: '端口' },
  { dataIndex: 'accessType', key: 'accessType', title: '接入方式' },
  { dataIndex: 'status', key: 'status', title: '状态' },
  { dataIndex: 'healthStatus', key: 'healthStatus', title: '健康度' },
  { dataIndex: 'accessUrl', key: 'accessUrl', title: '访问地址' },
  { key: 'actions', title: '操作', width: 280 },
];

function tagColor(status?: string) {
  switch (status) {
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

const { connectionLabel } = useNetbridgeRealtime({
  onServiceStatus(payload) {
    const target = rows.value.find((item) => item.id === payload.serviceId || item.id === payload.entityId);
    if (!target) return;
    target.status = payload.status || target.status;
    target.healthStatus = payload.healthStatus || target.healthStatus;
  },
});

function openCreate() {
  editingId.value = null;
  Object.assign(form, {
    accessType: 'tailscale',
    domain: '',
    name: '',
    port: 80,
    protocol: 'HTTP',
    serverId: 1,
    targetHost: '127.0.0.1',
    type: 'Web',
  });
  modalOpen.value = true;
}

function openEdit(record: any) {
  editingId.value = record.id;
  Object.assign(form, {
    accessType: record.accessType,
    domain: record.domain || '',
    name: record.name,
    port: record.port,
    protocol: record.protocol,
    serverId: record.serverId,
    targetHost: record.targetHost || '127.0.0.1',
    type: record.type,
  });
  modalOpen.value = true;
}

async function submitForm() {
  saving.value = true;
  try {
    if (editingId.value) {
      await updateService({
        ...form,
        enabled: 0,
        healthStatus: 'unknown',
        id: editingId.value,
        status: 'inactive',
      });
      message.success('服务已更新');
    } else {
      await createService(form);
      message.success('服务已创建');
    }
    modalOpen.value = false;
    await loadData();
  } finally {
    saving.value = false;
  }
}

async function handleAction(action: 'check' | 'restart' | 'start' | 'stop', id: number) {
  const map = { check: checkService, restart: restartService, start: startService, stop: stopService };
  await map[action](id);
  message.success(`已创建 ${action.toUpperCase()} 任务`);
  await loadData();
}

async function handleDelete(id: number) {
  await deleteService(id);
  message.success('服务已删除');
  await loadData();
}

async function loadData() {
  loading.value = true;
  try {
    const data = await getServiceList(query);
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
      <div class="mb-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h2 class="text-xl font-semibold text-slate-900">服务管理</h2>
          <p class="mt-1 text-sm text-slate-500">管理内网服务的映射、健康度和任务驱动动作。</p>
        </div>
        <Space>
          <Tag :color="connectionLabel === 'live' ? 'success' : connectionLabel === 'connecting' ? 'processing' : 'default'">
            {{ connectionLabel }}
          </Tag>
          <Button type="primary" @click="openCreate">新建服务</Button>
        </Space>
      </div>

      <Space class="mb-4" wrap>
        <Input v-model:value="query.type" allow-clear placeholder="按类型筛选" style="width: 180px" />
        <Select
          v-model:value="query.status"
          allow-clear
          placeholder="状态"
          style="width: 160px"
          :options="[
            { label: 'active', value: 'active' },
            { label: 'inactive', value: 'inactive' },
            { label: 'error', value: 'error' },
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
          <template v-if="column.key === 'name'">
            <button class="font-medium text-left text-sky-600 hover:text-sky-500" @click="router.push(`/netbridge/services/${record.id}`)">
              {{ record.name }}
            </button>
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="tagColor(record.status)">{{ record.status }}</Tag>
          </template>
          <template v-else-if="column.key === 'healthStatus'">
            <Tag :color="tagColor(record.healthStatus)">{{ record.healthStatus }}</Tag>
          </template>
          <template v-else-if="column.key === 'accessUrl'">
            <div class="max-w-[280px] truncate text-xs text-slate-600">{{ record.accessUrl || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'actions'">
            <Space wrap>
              <Button size="small" @click="openEdit(record)">编辑</Button>
              <Button size="small" type="primary" ghost @click="handleAction('start', record.id)">启动</Button>
              <Button size="small" @click="handleAction('stop', record.id)">停止</Button>
              <Button size="small" @click="handleAction('restart', record.id)">重启</Button>
              <Button size="small" @click="handleAction('check', record.id)">检测</Button>
              <Popconfirm title="确定删除该服务？" @confirm="handleDelete(record.id)">
                <Button danger size="small">删除</Button>
              </Popconfirm>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <Modal v-model:open="modalOpen" :confirm-loading="saving" :title="editingId ? '编辑服务' : '新建服务'" @ok="submitForm">
      <Form layout="vertical">
        <Form.Item label="服务名称">
          <Input v-model:value="form.name" />
        </Form.Item>
        <Form.Item label="服务类型">
          <Input v-model:value="form.type" />
        </Form.Item>
        <Form.Item label="协议">
          <Select
            v-model:value="form.protocol"
            :options="[
              { label: 'HTTP', value: 'HTTP' },
              { label: 'HTTPS', value: 'HTTPS' },
              { label: 'TCP', value: 'TCP' },
            ]"
          />
        </Form.Item>
        <Form.Item label="端口">
          <InputNumber v-model:value="form.port" class="w-full" />
        </Form.Item>
        <Form.Item label="接入方式">
          <Select
            v-model:value="form.accessType"
            :options="[
              { label: 'tailscale', value: 'tailscale' },
              { label: 'tunnel', value: 'tunnel' },
            ]"
          />
        </Form.Item>
        <Form.Item label="目标地址">
          <Input v-model:value="form.targetHost" />
        </Form.Item>
        <Form.Item label="域名">
          <Input v-model:value="form.domain" />
        </Form.Item>
        <Form.Item label="服务器 ID">
          <InputNumber v-model:value="form.serverId" class="w-full" />
        </Form.Item>
      </Form>
    </Modal>
  </div>
</template>
