<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { Button, Card, Descriptions, Drawer, Input, Select, Space, Table, Tag } from 'antdv-next';

import { getOperationLogList } from '#/api/netbridge/log';

const router = useRouter();
const loading = ref(false);
const rows = ref<any[]>([]);
const total = ref(0);
const detailOpen = ref(false);
const currentRecord = ref<any | null>(null);
const timeRangeHours = ref(24);
const query = reactive({
  action: '',
  pageNo: 1,
  pageSize: 10,
  resourceType: '',
  result: undefined as string | undefined,
});

const columns = [
  { dataIndex: 'action', key: 'action', title: '动作' },
  { dataIndex: 'resourceType', key: 'resourceType', title: '资源类型' },
  { dataIndex: 'resourceId', key: 'resourceId', title: '资源 ID' },
  { dataIndex: 'operatorName', key: 'operatorName', title: '操作人' },
  { dataIndex: 'result', key: 'result', title: '结果' },
  { dataIndex: 'detail', key: 'detail', title: '详情' },
  { dataIndex: 'createdAt', key: 'createdAt', title: '时间' },
  { dataIndex: 'actions', key: 'actions', title: '操作', width: 100 },
];

const filteredRows = computed(() => {
  const cutoff = Date.now() - timeRangeHours.value * 60 * 60 * 1000;
  return rows.value.filter((item) => {
    const timestamp = item.createdAt ? new Date(item.createdAt).getTime() : 0;
    return !timestamp || timestamp >= cutoff;
  });
});

const summary = computed(() => {
  const counts = filteredRows.value.reduce(
    (acc, item) => {
      acc[item.result] = (acc[item.result] || 0) + 1;
      acc.resourceTypes[item.resourceType] = (acc.resourceTypes[item.resourceType] || 0) + 1;
      return acc;
    },
    { failed: 0, resourceTypes: {} as Record<string, number>, success: 0 },
  );
  const topResourceType =
    Object.entries(counts.resourceTypes).sort((a, b) => b[1] - a[1])[0]?.[0] || '--';
  return [
    { label: '成功记录', value: counts.success, color: 'success' },
    { label: '失败记录', value: counts.failed, color: 'error' },
    { label: '当前窗口总数', value: filteredRows.value.length, color: 'processing' },
    { label: '高频资源', value: topResourceType, color: 'default' },
  ];
});

function resultColor(result?: string) {
  switch (result) {
    case 'success':
      return 'success';
    case 'failed':
      return 'error';
    default:
      return 'default';
  }
}

function openDetail(record: any) {
  currentRecord.value = record;
  detailOpen.value = true;
}

function jumpToResource(record: any) {
  if (!record?.resourceType) return;
  switch (record.resourceType) {
    case 'server':
      if (record.resourceId) {
        router.push(`/netbridge/servers/${record.resourceId}`);
      }
      break;
    case 'service':
      if (record.resourceId) {
        router.push(`/netbridge/services/${record.resourceId}`);
      }
      break;
    case 'task':
      router.push('/netbridge/tasks');
      break;
    case 'install_token':
      router.push('/netbridge/install-tokens');
      break;
    default:
      break;
  }
}

async function loadData() {
  loading.value = true;
  try {
    const data = await getOperationLogList(query);
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
          <h2 class="text-xl font-semibold text-slate-900">操作审计</h2>
          <p class="mt-1 text-sm text-slate-500">追踪核心资源的创建、变更、失败与人工操作轨迹。</p>
        </div>
      </div>

      <Space class="mb-4" wrap>
        <Input v-model:value="query.action" allow-clear placeholder="动作" style="width: 180px" />
        <Input v-model:value="query.resourceType" allow-clear placeholder="资源类型" style="width: 180px" />
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
        <Select
          v-model:value="query.result"
          allow-clear
          placeholder="结果"
          style="width: 160px"
          :options="[
            { label: '成功', value: 'success' },
            { label: '失败', value: 'failed' },
          ]"
        />
        <Button type="primary" @click="loadData">查询</Button>
      </Space>

      <Table
        :columns="columns"
        :data-source="filteredRows"
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
          <template v-if="column.key === 'operatorName'">
            <div class="font-medium text-slate-900">{{ record.operatorName || 'system' }}</div>
            <div class="text-xs text-slate-500">UID: {{ record.operatorId ?? '-' }}</div>
          </template>
          <template v-else-if="column.key === 'result'">
            <Tag :color="resultColor(record.result)">{{ record.result }}</Tag>
          </template>
          <template v-else-if="column.key === 'detail'">
            <div class="max-w-[360px] truncate text-slate-600">{{ record.detail || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'actions'">
            <Button size="small" type="link" @click="openDetail(record)">详情</Button>
          </template>
        </template>
      </Table>
    </Card>

    <Drawer v-model:open="detailOpen" title="审计详情" width="640">
      <template v-if="currentRecord">
        <div class="space-y-4">
          <Card size="small" title="结果概览">
            <div class="grid grid-cols-2 gap-3 text-sm text-slate-600">
              <div>动作：{{ currentRecord.action }}</div>
              <div>
                结果：
                <Tag :color="resultColor(currentRecord.result)">{{ currentRecord.result }}</Tag>
              </div>
              <div>资源类型：{{ currentRecord.resourceType }}</div>
              <div>资源 ID：{{ currentRecord.resourceId || '-' }}</div>
            </div>
          </Card>

          <Card size="small" title="操作上下文">
            <Descriptions :column="1" bordered size="small">
              <Descriptions.Item label="操作人">
                {{ currentRecord.operatorName || 'system' }}
              </Descriptions.Item>
              <Descriptions.Item label="操作人 ID">
                {{ currentRecord.operatorId ?? '-' }}
              </Descriptions.Item>
              <Descriptions.Item label="时间">
                {{ currentRecord.createdAt }}
              </Descriptions.Item>
              <Descriptions.Item label="详情">
                <div class="break-all text-slate-700">
                  {{ currentRecord.detail || '--' }}
                </div>
              </Descriptions.Item>
            </Descriptions>
          </Card>

          <Card size="small" title="关联跳转">
            <Space wrap>
              <Button
                type="primary"
                @click="jumpToResource(currentRecord)"
              >
                查看关联资源
              </Button>
              <Tag color="default">{{ currentRecord.resourceType }}</Tag>
            </Space>
          </Card>
        </div>
      </template>
    </Drawer>
  </div>
</template>
