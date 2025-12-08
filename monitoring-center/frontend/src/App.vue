<template>
  <el-container id="app">
    <el-header class="app-header">
      <div class="header-content">
        <div class="logo">
        
        </div>
        <el-menu 
          :default-active="activeTab" 
          mode="horizontal" 
          @select="handleTabChange"
          class="app-menu"
        >
          <el-menu-item index="dashboard">
            <el-icon><DataBoard /></el-icon>
            <span>监控台</span>
          </el-menu-item>
          <el-menu-item index="agents">
            <el-icon><Monitor /></el-icon>
            <span>代理管理</span>
          </el-menu-item>
          <el-menu-item index="metrics">
            <el-icon><TrendCharts /></el-icon>
            <span>监控指标</span>
          </el-menu-item>
          <el-menu-item index="alerts">
            <el-icon><Bell /></el-icon>
            <span>告警管理</span>
          </el-menu-item>
          <el-menu-item index="emergency">
            <el-icon><Reading /></el-icon>
            <span>应急知识库</span>
          </el-menu-item>
        </el-menu>
      </div>
    </el-header>
    
    <el-main class="app-main">
      <MonitoringDashboard v-if="activeTab === 'dashboard'" />
      <AgentManagement v-if="activeTab === 'agents'" />
      <MetricCollection v-if="activeTab === 'metrics'" />
      <AlertManagement v-if="activeTab === 'alerts'" />
      <EmergencyKnowledge v-if="activeTab === 'emergency'" />
    </el-main>
  </el-container>
</template>

<script>
import { Monitor, DataBoard, TrendCharts, Bell, Reading } from '@element-plus/icons-vue'
import AgentManagement from './components/AgentManagement.vue'
import MetricCollection from './components/MetricCollection.vue'
import AlertManagement from './components/AlertManagement.vue'
import MonitoringDashboard from './components/MonitoringDashboard.vue'
import EmergencyKnowledge from './components/EmergencyKnowledge.vue'

export default {
  name: 'App',
  components: {
    Monitor,
    DataBoard,
    TrendCharts,
    Bell,
    Reading,
    MonitoringDashboard,
    AgentManagement,
    MetricCollection,
    AlertManagement,
    EmergencyKnowledge
  },
  data() {
    return {
      activeTab: 'dashboard'
    }
  },
  methods: {
    handleTabChange(key) {
      this.activeTab = key
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  background: #f5f7fa;
  min-height: 100vh;
}

#app {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  color: #000000;
  min-height: 100vh;
}

.app-header {
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  padding: 0 !important;
  height: auto !important;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px 0;
}

.logo h1 {
  color: #1976d2;
  font-size: 24px;
  margin: 0;
  font-weight: 600;
}

.app-menu {
  flex: 1;
  border-bottom: none;
  background: transparent;
}

.app-menu .el-menu-item {
  font-weight: 500;
}

.app-main {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 80px);
}

/* Override Element Plus menu styles */
.el-menu--horizontal .el-menu-item:not(.is-disabled):hover,
.el-menu--horizontal .el-menu-item:not(.is-disabled):focus {
  background-color: #ecf5ff;
}

.el-menu--horizontal .el-menu-item.is-active {
  border-bottom-color: #1976d2;
  color: #1976d2;
}
</style>