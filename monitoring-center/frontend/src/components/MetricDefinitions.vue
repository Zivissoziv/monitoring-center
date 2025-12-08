<template>
  <div class="metric-definitions">

    <!-- Action Bar -->
    <el-card shadow="hover" style="margin-bottom: 20px;">
      <el-alert type="info" :closable="false" style="margin-bottom: 15px;">
        <template #title>
          <strong>使用说明</strong>
        </template>
        启用指标定义后，系统将自动为所有现有代理创建配置。点击表格左侧的<strong>展开按钮</strong>可查看和配置每个代理的采集设置。
      </el-alert>
      <el-button type="primary" @click="openCreateDialog" :icon="Plus">新建指标定义</el-button>
      <el-button @click="loadDefinitions" :icon="Refresh">刷新</el-button>
    </el-card>

    <!-- Definitions Table -->
    <el-card shadow="hover">
      <el-table :data="definitions" stripe style="width: 100%" v-loading="loading" row-key="id">
        <el-table-column type="expand">
          <template #default="props">
            <div style="padding: 20px;">
              <h4 style="margin-bottom: 15px; color: #1976d2;">代理配置情况</h4>
              <el-table :data="getAgentConfigsForMetric(props.row.metricName)" size="small" border>
                <el-table-column label="代理名称" width="200">
                  <template #default="scope">
                    {{ getAgentName(scope.row.agentId) }}
                  </template>
                </el-table-column>
                <el-table-column label="代理IP" width="150">
                  <template #default="scope">
                    {{ getAgentIp(scope.row.agentId) }}
                  </template>
                </el-table-column>
                <el-table-column label="默认间隔(秒)" width="120" align="center">
                  <template #default="scope">
                    {{ props.row.collectionInterval }}
                  </template>
                </el-table-column>
                <el-table-column label="自定义间隔(秒)" width="180" align="center">
                  <template #default="scope">
                    <el-input-number
                      v-model="scope.row.customInterval"
                      :min="5"
                      :max="3600"
                      :step="5"
                      size="small"
                      @change="updateAgentConfig(scope.row)"
                      placeholder="使用默认"
                      style="width: 140px;"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="启用状态" width="100" align="center">
                  <template #default="scope">
                    <el-switch
                      v-model="scope.row.enabled"
                      @change="updateAgentConfig(scope.row)"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="最后采集" min-width="180">
                  <template #default="scope">
                    {{ scope.row.lastCollectionTime ? formatTime(scope.row.lastCollectionTime) : '未采集' }}
                  </template>
                </el-table-column>
                <template #empty>
                  <el-empty description="暂无代理配置" :image-size="60" />
                </template>
              </el-table>
              <div style="margin-top: 15px;">
                <el-button size="small" type="primary" @click="syncMetricToAllAgents(props.row.metricName)" :icon="Refresh">
                  同步所有代理
                </el-button>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="metricName" label="指标名称" min-width="150" />
        <el-table-column prop="displayName" label="显示名称" min-width="150" />
        <el-table-column label="采集命令" min-width="300">
          <template #default="scope">
            <el-tooltip :content="scope.row.collectionCommand" placement="top">
              <code class="command-preview">{{ truncate(scope.row.collectionCommand, 50) }}</code>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="collectionInterval" label="采集间隔(秒)" width="120" align="center" />
        <el-table-column prop="unit" label="单位" width="100" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.enabled ? 'success' : 'info'" size="small">
              {{ scope.row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="openEditDialog(scope.row)" :icon="Edit">编辑</el-button>
            <el-popconfirm 
              title="确定要删除该指标定义吗？" 
              @confirm="deleteDefinition(scope.row.id)"
            >
              <template #reference>
                <el-button size="small" type="danger" :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无指标定义" />
        </template>
      </el-table>
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog 
      v-model="showDialog" 
      :title="isEditMode ? '编辑指标定义' : '新建指标定义'" 
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form :model="currentDefinition" label-width="120px">
        <el-form-item label="指标名称" required>
          <el-input v-model="currentDefinition.metricName" placeholder="例如: CPU, MEMORY, DISK_IO" />
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">
            唯一标识符，建议使用大写字母和下划线
          </div>
        </el-form-item>
        
        <el-form-item label="显示名称" required>
          <el-input v-model="currentDefinition.displayName" placeholder="例如: CPU使用率" />
        </el-form-item>
        
        <el-form-item label="描述">
          <el-input v-model="currentDefinition.description" type="textarea" :rows="2" placeholder="详细描述该指标的含义" />
        </el-form-item>
        
        <el-form-item label="采集命令" required>
          <el-input 
            v-model="currentDefinition.collectionCommand" 
            type="textarea" 
            :rows="3" 
            placeholder="例如: free | grep Mem | awk '{print ($3/$2) * 100.0}'"
            class="command-input"
          />
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">
            Shell命令，在代理服务器上执行以采集指标
          </div>
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="采集间隔">
              <el-input-number v-model="currentDefinition.collectionInterval" :min="5" :max="3600" :step="5" />
              <span style="margin-left: 10px;">秒</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位">
              <el-input v-model="currentDefinition.unit" placeholder="例如: %, MB, count" style="width: 150px;" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="加工规则">
          <el-input 
            v-model="currentDefinition.processingRule" 
            type="textarea" 
            :rows="3" 
            placeholder="JavaScript表达式，例如: parseFloat(value) 或 value.split(':')[1]"
            class="command-input"
          />
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">
            可选，使用JavaScript处理命令输出，变量 'output' 或 'value' 包含原始输出
          </div>
        </el-form-item>
        
        <el-form-item label="状态">
          <el-switch v-model="currentDefinition.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="saveDefinition">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { TrendCharts, Plus, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'MetricDefinitions',
  components: {
    TrendCharts,
    Plus,
    Edit,
    Delete,
    Refresh
  },
  data() {
    return {
      definitions: [],
      agents: [],
      agentConfigs: [], // All agent-metric configs
      loading: false,
      showDialog: false,
      isEditMode: false,
      currentDefinition: {
        id: null,
        metricName: '',
        displayName: '',
        description: '',
        collectionCommand: '',
        collectionInterval: 30,
        processingRule: '',
        unit: '%',
        enabled: true
      }
    }
  },
  mounted() {
    this.loadDefinitions()
    this.loadAgents()
    this.loadAllAgentConfigs()
  },
  methods: {
    async loadDefinitions() {
      this.loading = true
      try {
        const response = await fetch('/api/metric-definitions')
        this.definitions = await response.json()
      } catch (error) {
        console.error('Error loading definitions:', error)
        ElMessage.error('加载指标定义失败')
      } finally {
        this.loading = false
      }
    },
    
    async loadAgents() {
      try {
        const response = await fetch('/api/agents')
        this.agents = await response.json()
      } catch (error) {
        console.error('Error loading agents:', error)
      }
    },
    
    async loadAllAgentConfigs() {
      try {
        const response = await fetch('/api/agent-metric-configs')
        this.agentConfigs = await response.json()
      } catch (error) {
        console.error('Error loading agent configs:', error)
      }
    },
    
    openCreateDialog() {
      this.isEditMode = false
      this.resetCurrentDefinition()
      this.showDialog = true
    },
    
    openEditDialog(definition) {
      this.isEditMode = true
      this.currentDefinition = {
        id: definition.id,
        metricName: definition.metricName,
        displayName: definition.displayName,
        description: definition.description || '',
        collectionCommand: definition.collectionCommand,
        collectionInterval: definition.collectionInterval,
        processingRule: definition.processingRule || '',
        unit: definition.unit,
        enabled: definition.enabled
      }
      this.showDialog = true
    },
    
    closeDialog() {
      this.showDialog = false
      this.resetCurrentDefinition()
    },
    
    resetCurrentDefinition() {
      this.currentDefinition = {
        id: null,
        metricName: '',
        displayName: '',
        description: '',
        collectionCommand: '',
        collectionInterval: 30,
        processingRule: '',
        unit: '%',
        enabled: true
      }
    },
    
    async saveDefinition() {
      if (!this.currentDefinition.metricName || !this.currentDefinition.displayName || 
          !this.currentDefinition.collectionCommand) {
        ElMessage.warning('请填写必填项')
        return
      }
      
      try {
        const url = this.isEditMode 
          ? `/api/metric-definitions/${this.currentDefinition.id}`
          : '/api/metric-definitions'
        
        const method = this.isEditMode ? 'PUT' : 'POST'
        
        const response = await fetch(url, {
          method: method,
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(this.currentDefinition)
        })
        
        if (response.ok) {
          ElMessage.success(this.isEditMode ? '更新成功' : '创建成功')
          this.closeDialog()
          await this.loadDefinitions()
          await this.loadAllAgentConfigs() // Reload configs after save
        } else {
          ElMessage.error(this.isEditMode ? '更新失败' : '创建失败')
        }
      } catch (error) {
        console.error('Error saving definition:', error)
        ElMessage.error('保存失败')
      }
    },
    
    async deleteDefinition(id) {
      try {
        const response = await fetch(`/api/metric-definitions/${id}`, {
          method: 'DELETE'
        })
        
        if (response.ok) {
          ElMessage.success('删除成功')
          this.loadDefinitions()
        } else {
          ElMessage.error('删除失败')
        }
      } catch (error) {
        console.error('Error deleting definition:', error)
        ElMessage.error('删除失败')
      }
    },
    
    truncate(text, length) {
      if (!text) return ''
      return text.length > length ? text.substring(0, length) + '...' : text
    },
    
    // Agent config methods
    getAgentConfigsForMetric(metricName) {
      return this.agentConfigs.filter(config => config.metricName === metricName)
    },
    
    getAgentName(agentId) {
      const agent = this.agents.find(a => a.id === agentId)
      return agent ? agent.name : '未知代理'
    },
    
    getAgentIp(agentId) {
      const agent = this.agents.find(a => a.id === agentId)
      return agent ? agent.ip : '-'
    },
    
    async updateAgentConfig(config) {
      try {
        const response = await fetch('/api/agent-metric-configs', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(config)
        })
        
        if (response.ok) {
          ElMessage.success('配置已更新')
          await this.loadAllAgentConfigs()
        } else {
          ElMessage.error('更新失败')
        }
      } catch (error) {
        console.error('Error updating config:', error)
        ElMessage.error('更新失败')
      }
    },
    
    async syncMetricToAllAgents(metricName) {
      try {
        // Get all agents and create/update configs for this metric
        const promises = this.agents.map(agent => 
          fetch('/api/agent-metric-configs', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json'
            },
            body: JSON.stringify({
              agentId: agent.id,
              metricName: metricName,
              enabled: true
            })
          })
        )
        
        await Promise.all(promises)
        ElMessage.success('同步成功')
        await this.loadAllAgentConfigs()
      } catch (error) {
        console.error('Error syncing metric:', error)
        ElMessage.error('同步失败')
      }
    },
    
    formatTime(timestamp) {
      return new Date(timestamp).toLocaleString('zh-CN')
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

.command-input {
  font-family: 'Courier New', monospace;
}

.command-input :deep(textarea) {
  font-family: 'Courier New', monospace;
  font-size: 13px;
}
</style>
