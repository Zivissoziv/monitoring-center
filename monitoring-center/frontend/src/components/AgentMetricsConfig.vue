<template>
  <div class="agent-metrics-config">
    <el-page-header @back="$emit('back')" class="page-header">
      <template #content>
        <div class="page-title">
          <el-icon :size="24"><Link /></el-icon>
          <span>代理指标配置</span>
        </div>
      </template>
    </el-page-header>

    <!-- Agent Selector -->
    <el-card shadow="hover" style="margin-bottom: 20px;">
      <el-form :inline="true">
        <el-form-item label="选择代理">
          <el-select v-model="selectedAgentId" @change="loadAgentConfigs" placeholder="请选择代理" style="width: 300px;">
            <el-option
              v-for="agent in agents"
              :key="agent.id"
              :label="`${agent.name} (${agent.ip}:${agent.port})`"
              :value="agent.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="syncAllMetrics" :loading="syncing" :icon="Refresh">
            同步所有指标
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Configs Table -->
    <el-card shadow="hover" v-if="selectedAgentId">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: 600;">指标配置列表</span>
          <el-tag :type="selectedAgent ? 'success' : 'info'" size="small">
            {{ selectedAgent ? selectedAgent.name : '未选择' }}
          </el-tag>
        </div>
      </template>
      
      <el-table :data="configs" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="metricName" label="指标名称" width="150" />
        <el-table-column label="显示名称" width="150">
          <template #default="scope">
            {{ getMetricDisplayName(scope.row.metricName) }}
          </template>
        </el-table-column>
        <el-table-column label="采集命令" min-width="300">
          <template #default="scope">
            <el-tooltip :content="getMetricCommand(scope.row.metricName)" placement="top">
              <code class="command-preview">{{ truncate(getMetricCommand(scope.row.metricName), 60) }}</code>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="默认间隔(秒)" width="120" align="center">
          <template #default="scope">
            {{ getMetricInterval(scope.row.metricName) }}
          </template>
        </el-table-column>
        <el-table-column label="自定义间隔(秒)" width="150" align="center">
          <template #default="scope">
            <el-input-number
              v-model="scope.row.customInterval"
              :min="5"
              :max="3600"
              :step="5"
              size="small"
              @change="updateConfig(scope.row)"
              placeholder="使用默认"
            />
          </template>
        </el-table-column>
        <el-table-column label="启用状态" width="120" align="center">
          <template #default="scope">
            <el-switch
              v-model="scope.row.enabled"
              @change="updateConfig(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="最后采集时间" width="180">
          <template #default="scope">
            {{ scope.row.lastCollectionTime ? formatTime(scope.row.lastCollectionTime) : '未采集' }}
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无配置，请点击'同步所有指标'按钮初始化" />
        </template>
      </el-table>
    </el-card>

    <el-card shadow="hover" v-else style="margin-top: 20px;">
      <el-empty description="请先选择一个代理" />
    </el-card>
  </div>
</template>

<script>
import { Link, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'AgentMetricsConfig',
  components: {
    Link,
    Refresh
  },
  data() {
    return {
      agents: [],
      metricDefinitions: [],
      configs: [],
      selectedAgentId: null,
      loading: false,
      syncing: false
    }
  },
  computed: {
    selectedAgent() {
      return this.agents.find(a => a.id === this.selectedAgentId)
    }
  },
  mounted() {
    this.loadAgents()
    this.loadMetricDefinitions()
  },
  methods: {
    async loadAgents() {
      try {
        const response = await fetch('/api/agents')
        this.agents = await response.json()
      } catch (error) {
        console.error('Error loading agents:', error)
        ElMessage.error('加载代理列表失败')
      }
    },
    
    async loadMetricDefinitions() {
      try {
        const response = await fetch('/api/metric-definitions')
        this.metricDefinitions = await response.json()
      } catch (error) {
        console.error('Error loading metric definitions:', error)
      }
    },
    
    async loadAgentConfigs() {
      if (!this.selectedAgentId) return
      
      this.loading = true
      try {
        const response = await fetch(`/api/agent-metric-configs/agent/${this.selectedAgentId}`)
        this.configs = await response.json()
      } catch (error) {
        console.error('Error loading configs:', error)
        ElMessage.error('加载配置失败')
      } finally {
        this.loading = false
      }
    },
    
    async syncAllMetrics() {
      if (!this.selectedAgentId) {
        ElMessage.warning('请先选择代理')
        return
      }
      
      this.syncing = true
      try {
        const response = await fetch(`/api/agent-metric-configs/agent/${this.selectedAgentId}/initialize`, {
          method: 'POST'
        })
        
        if (response.ok) {
          ElMessage.success('同步成功')
          await this.loadAgentConfigs()
        } else {
          ElMessage.error('同步失败')
        }
      } catch (error) {
        console.error('Error syncing metrics:', error)
        ElMessage.error('同步失败')
      } finally {
        this.syncing = false
      }
    },
    
    async updateConfig(config) {
      try {
        const response = await fetch('/api/agent-metric-configs', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            agentId: this.selectedAgentId,
            metricName: config.metricName,
            enabled: config.enabled,
            customInterval: config.customInterval
          })
        })
        
        if (response.ok) {
          ElMessage.success('配置已更新')
        } else {
          ElMessage.error('更新失败')
        }
      } catch (error) {
        console.error('Error updating config:', error)
        ElMessage.error('更新失败')
      }
    },
    
    getMetricDisplayName(metricName) {
      const definition = this.metricDefinitions.find(d => d.metricName === metricName)
      return definition ? definition.displayName : metricName
    },
    
    getMetricCommand(metricName) {
      const definition = this.metricDefinitions.find(d => d.metricName === metricName)
      return definition ? definition.collectionCommand : ''
    },
    
    getMetricInterval(metricName) {
      const definition = this.metricDefinitions.find(d => d.metricName === metricName)
      return definition ? definition.collectionInterval : 30
    },
    
    formatTime(timestamp) {
      return new Date(timestamp).toLocaleString('zh-CN')
    },
    
    truncate(text, length) {
      if (!text) return ''
      return text.length > length ? text.substring(0, length) + '...' : text
    }
  }
}
</script>

<style scoped>
.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: #1976d2;
}

.page-header {
  margin-bottom: 24px;
}

.command-preview {
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #4caf50;
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
