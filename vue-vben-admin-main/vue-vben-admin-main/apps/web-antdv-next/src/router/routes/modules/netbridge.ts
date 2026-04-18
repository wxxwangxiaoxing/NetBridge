import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:tower-control',
      order: -2,
      title: $t('page.netbridge.title'),
    },
    name: 'NetBridge',
    path: '/netbridge',
    children: [
      {
        name: 'NetBridgeOverview',
        path: '/netbridge/overview',
        component: () => import('#/views/netbridge/overview/index.vue'),
        meta: {
          affixTab: true,
          icon: 'lucide:radar',
          title: $t('page.netbridge.overview'),
        },
      },
      {
        name: 'NetBridgeServers',
        path: '/netbridge/servers',
        component: () => import('#/views/netbridge/servers/index.vue'),
        meta: {
          icon: 'lucide:server-cog',
          title: $t('page.netbridge.servers'),
        },
      },
      {
        name: 'NetBridgeServerDetail',
        path: '/netbridge/servers/:id',
        component: () => import('#/views/netbridge/servers/detail.vue'),
        meta: {
          hideInMenu: true,
          title: $t('page.netbridge.serverDetail'),
        },
      },
      {
        name: 'NetBridgeServices',
        path: '/netbridge/services',
        component: () => import('#/views/netbridge/services/index.vue'),
        meta: {
          icon: 'lucide:network',
          title: $t('page.netbridge.services'),
        },
      },
      {
        name: 'NetBridgeServiceDetail',
        path: '/netbridge/services/:id',
        component: () => import('#/views/netbridge/services/detail.vue'),
        meta: {
          hideInMenu: true,
          title: $t('page.netbridge.serviceDetail'),
        },
      },
      {
        name: 'NetBridgeTasks',
        path: '/netbridge/tasks',
        component: () => import('#/views/netbridge/tasks/index.vue'),
        meta: {
          icon: 'lucide:list-checks',
          title: $t('page.netbridge.tasks'),
        },
      },
      {
        name: 'NetBridgeMonitor',
        path: '/netbridge/monitor',
        component: () => import('#/views/netbridge/monitor/index.vue'),
        meta: {
          icon: 'lucide:activity',
          title: $t('page.netbridge.monitor'),
        },
      },
      {
        name: 'NetBridgeLogs',
        path: '/netbridge/logs',
        component: () => import('#/views/netbridge/logs/index.vue'),
        meta: {
          icon: 'lucide:scroll-text',
          title: $t('page.netbridge.logs'),
        },
      },
      {
        name: 'NetBridgeInstallTokens',
        path: '/netbridge/install-tokens',
        component: () => import('#/views/netbridge/install-tokens/index.vue'),
        meta: {
          icon: 'lucide:key-round',
          title: $t('page.netbridge.installTokens'),
        },
      },
      {
        name: 'NetBridgeOperationAudit',
        path: '/netbridge/operation-audit',
        component: () => import('#/views/netbridge/operation-audit/index.vue'),
        meta: {
          icon: 'lucide:file-search-2',
          title: $t('page.netbridge.operationAudit'),
        },
      },
    ],
  },
];

export default routes;
