<template>
  <div class="metric-definitions">

    <!-- Search and Filter Bar -->
    <el-card shadow="hover" style="margin-bottom: 20px;">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="指标名称">
          <el-input v-model="searchForm.metricName" placeholder="指标名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="searchForm.displayName" placeholder="显示名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="指标类型">
          <el-select v-model="searchForm.metricType" placeholder="全部" clearable style="width: 150px">
            <el-option label="数值型" value="NUMERIC" />
            <el-option label="布尔型" value="BOOLEAN" />
            <el-option label="字符型" value="STRING" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.enabled" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="true" />
            <el-option label="禁用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :icon="Search">查询</el-button>
          <el-button @click="handleReset" :icon="RefreshIcon">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Definitions Table -->
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>指标定义列表</span>
          <el-button type="primary" @click="openCreateDialog" :icon="Plus">新建指标定义</el-button>
        </div>
      </template>
      <el-table :data="paginatedDefinitions" stripe style="width: 100%" v-loading="loading" row-key="id">
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
        <el-table-column label="指标类型" width="100" align="center">
          <template #default="scope">
            <el-tag :type="getMetricTypeTagType(scope.row.metricType)" size="small">
              {{ getMetricTypeText(scope.row.metricType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="采集命令" min-width="300">
          <template #default="scope">
            <el-tooltip :content="scope.row.collectionCommand" placement="top">
              <code class="command-preview">{{ truncate(scope.row.collectionCommand, 50) }}</code>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="collectionInterval" label="采集间隔(秒)" width="120" align="center" />
        <el-table-column label="单位" width="100" align="center">
          <template #default="scope">
            <span v-if="scope.row.metricType === 'NUMERIC'">{{ scope.row.unit || '-' }}</span>
            <span v-else style="color: #909399;">-</span>
          </template>
        </el-table-column>
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
      
      <!-- Pagination -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredTotal"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
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
        
        <el-form-item label="指标类型" required>
          <el-select v-model="currentDefinition.metricType" placeholder="选择指标类型" @change="onMetricTypeChangeInForm">
            <el-option label="数值型" value="NUMERIC">
              <span>数值型</span>
              <span style="color: #909399; font-size: 12px; margin-left: 10px;">用于数值数据，如CPU、内存使用率</span>
            </el-option>
            <el-option label="布尔型" value="BOOLEAN">
              <span>布尔型</span>
              <span style="color: #909399; font-size: 12px; margin-left: 10px;">用于真/假数据，如端口状态</span>
            </el-option>
            <el-option label="字符型" value="STRING">
              <span>字符型</span>
              <span style="color: #909399; font-size: 12px; margin-left: 10px;">用于文本数据，如应用状态</span>
            </el-option>
          </el-select>
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">
            选择指标的数据类型，将影响告警规则的条件匹配方式
          </div>
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
          <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 8px;">
            <div style="font-size: 12px; color: #909399;">
              Shell命令，在代理服务器上执行以采集指标
            </div>
            <el-button 
              size="small" 
              type="primary" 
              :icon="Promotion"
              @click="openTestCommandDialog"
              :disabled="!currentDefinition.collectionCommand"
            >
              测试命令
            </el-button>
          </div>
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="采集间隔">
              <el-input-number v-model="currentDefinition.collectionInterval" :min="5" :max="3600" :step="5" />
              <span style="margin-left: 10px;">秒</span>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="currentDefinition.metricType === 'NUMERIC'">
            <el-form-item label="单位">
              <el-input v-model="currentDefinition.unit" placeholder="例如: %, MB, count" style="width: 150px;" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="状态">
          <el-switch v-model="currentDefinition.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="saveDefinition">保存</el-button>
      </template>
    </el-dialog>

    <!-- Test Command Dialog -->
    <el-dialog 
      v-model="showTestDialog" 
      title="测试采集命令" 
      width="700px"
      :close-on-click-modal="false"
    >
      <el-alert type="info" :closable="false" style="margin-bottom: 15px;">
        <div style="font-size: 13px;">
          选择一个代理执行命令，系统将验证返回结果是否符合指定的指标类型
        </div>
      </el-alert>

      <el-form label-width="100px">
        <el-form-item label="指标类型">
          <el-tag :type="getMetricTypeTagType(currentDefinition.metricType)" size="large">
            {{ getMetricTypeText(currentDefinition.metricType) }}
          </el-tag>
        </el-form-item>

        <el-form-item label="采集命令">
          <code class="command-preview" style="display: block; padding: 10px; background: #f5f5f5; border-radius: 4px; white-space: pre-wrap; word-break: break-all;">
            {{ currentDefinition.collectionCommand }}
          </code>
        </el-form-item>

        <el-form-item label="选择代理" required>
          <el-select v-model="testAgentId" placeholder="请选择一个代理" style="width: 100%;">
            <el-option 
              v-for="agent in agents" 
              :key="agent.id" 
              :label="`${agent.name} (${agent.ip}:${agent.port})`" 
              :value="agent.id"
              :disabled="agent.status !== 'ACTIVE'"
            >
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span>{{ agent.name }}</span>
                <el-tag :type="agent.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                  {{ agent.status }}
                </el-tag>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button 
            type="primary" 
            @click="executeTestCommand" 
            :loading="testExecuting"
            :disabled="!testAgentId"
          >
            执行测试
          </el-button>
        </el-form-item>

        <!-- Test Result -->
        <el-form-item label="测试结果" v-if="testResult">
          <el-card shadow="never" :body-style="{ padding: '15px' }">
            <!-- Validation Result -->
            <div style="margin-bottom: 15px;">
              <el-alert 
                :type="testResult.valid ? 'success' : 'error'" 
                :closable="false"
                show-icon
              >
                <template #title>
                  <strong>{{ testResult.valid ? '✅ 验证通过' : '❌ 验证失败' }}</strong>
                </template>
                <div style="font-size: 13px; margin-top: 5px;">
                  {{ testResult.message }}
                </div>
              </el-alert>
            </div>

            <!-- Command Output -->
            <el-tabs>
              <el-tab-pane label="命令输出">
                <div style="background: #2c3e50; color: #ecf0f1; padding: 12px; border-radius: 4px; font-family: 'Courier New', monospace; font-size: 13px; max-height: 200px; overflow-y: auto;">
                  <div style="color: #95a5a6; margin-bottom: 5px;"># 退出码: {{ testResult.exitCode }}</div>
                  <div style="white-space: pre-wrap; word-break: break-all;">{{ testResult.output || '(无输出)' }}</div>
                </div>
              </el-tab-pane>
              <el-tab-pane label="错误输出" v-if="testResult.error">
                <div style="background: #2c3e50; color: #e74c3c; padding: 12px; border-radius: 4px; font-family: 'Courier New', monospace; font-size: 13px; max-height: 200px; overflow-y: auto;">
                  <div style="white-space: pre-wrap; word-break: break-all;">{{ testResult.error }}</div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </el-card>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="closeTestDialog">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { TrendCharts, Plus, Edit, Delete, Refresh, Promotion, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'MetricDefinitions',
  components: {
    TrendCharts,
    Plus,
    Edit,
    Delete,
    Refresh,
    Promotion,
    Search,
    RefreshIcon: Refresh
  },
  data() {
    return {
      definitions: [],
      agents: [],
      agentConfigs: [], // All agent-metric configs
      loading: false,
      searchForm: {
        metricName: '',
        displayName: '',
        metricType: '',
        enabled: ''
      },
      pagination: {
        currentPage: 1,
        pageSize: 10
      },
      showDialog: false,
      isEditMode: false,
      currentDefinition: {
        id: null,
        metricName: '',
        displayName: '',
        description: '',
        metricType: 'NUMERIC',
        collectionCommand: '',
        collectionInterval: 30,
        processingRule: '',
        unit: '%',
        enabled: true
      },
      // Test command dialog
      showTestDialog: false,
      testAgentId: null,
      testExecuting: false,
      testResult: null
    }
  },
  computed: {
    filteredDefinitions() {
      let filtered = this.definitions
      
      if (this.searchForm.metricName) {
        filtered = filtered.filter(def => 
          def.metricName.toLowerCase().includes(this.searchForm.metricName.toLowerCase())
        )
      }
      
      if (this.searchForm.displayName) {
        filtered = filtered.filter(def => 
          def.displayName.toLowerCase().includes(this.searchForm.displayName.toLowerCase())
        )
      }
      
      if (this.searchForm.metricType) {
        filtered = filtered.filter(def => 
          def.metricType === this.searchForm.metricType
        )
      }
      
      if (this.searchForm.enabled !== '') {
        filtered = filtered.filter(def => 
          def.enabled === this.searchForm.enabled
        )
      }
      
      return filtered
    },
    filteredTotal() {
      return this.filteredDefinitions.length
    },
    paginatedDefinitions() {
      const start = (this.pagination.currentPage - 1) * this.pagination.pageSize
      const end = start + this.pagination.pageSize
      return this.filteredDefinitions.slice(start, end)
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
    
    handleSearch() {
      this.pagination.currentPage = 1
    },
    
    handleReset() {
      this.searchForm = {
        metricName: '',
        displayName: '',
        metricType: '',
        enabled: ''
      }
      this.pagination.currentPage = 1
    },
    
    handleSizeChange(val) {
      this.pagination.pageSize = val
      this.pagination.currentPage = 1
    },
    
    handleCurrentChange(val) {
      this.pagination.currentPage = val
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
        metricType: definition.metricType || 'NUMERIC',
        collectionCommand: definition.collectionCommand,
        collectionInterval: definition.collectionInterval,
        processingRule: definition.processingRule || '',
        unit: definition.unit || (definition.metricType === 'NUMERIC' ? '%' : ''),
        enabled: definition.enabled
      }
      this.showDialog = true
    },
    
    closeDialog() {
      this.showDialog = false
      this.resetCurrentDefinition()
      // Clear test results
      this.testResult = null
      this.testAgentId = null
    },
    
    resetCurrentDefinition() {
      this.currentDefinition = {
        id: null,
        metricName: '',
        displayName: '',
        description: '',
        metricType: 'NUMERIC',
        collectionCommand: '',
        collectionInterval: 30,
        processingRule: '',
        unit: '%',
        enabled: true
      }
      // Clear test results when resetting
      this.testResult = null
      this.testAgentId = null
    },
    
    async saveDefinition() {
      if (!this.currentDefinition.metricName || !this.currentDefinition.displayName || 
          !this.currentDefinition.collectionCommand) {
        ElMessage.warning('请填写必填项')
        return
      }
      
      // Check if command has been tested and validated
      if (!this.isEditMode && (!this.testResult || !this.testResult.valid)) {
        ElMessage.warning('请先测试采集命令并确保验证通过')
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
    },
    
    getMetricTypeText(type) {
      const typeMap = {
        'NUMERIC': '数值型',
        'BOOLEAN': '布尔型',
        'STRING': '字符型'
      }
      return typeMap[type] || type
    },
    
    getMetricTypeTagType(type) {
      const tagTypeMap = {
        'NUMERIC': 'primary',
        'BOOLEAN': 'success',
        'STRING': 'warning'
      }
      return tagTypeMap[type] || 'info'
    },
    
    onMetricTypeChangeInForm() {
      // 根据指标类型设置默认单位
      if (this.currentDefinition.metricType === 'NUMERIC') {
        if (!this.currentDefinition.unit) {
          this.currentDefinition.unit = '%'
        }
      } else {
        // 布尔型和字符型不需要单位
        this.currentDefinition.unit = ''
      }
    },
    
    // Test command methods
    openTestCommandDialog() {
      this.showTestDialog = true
      this.testAgentId = null
      this.testResult = null
    },
    
    closeTestDialog() {
      this.showTestDialog = false
      this.testAgentId = null
      this.testResult = null
    },
    
    async executeTestCommand() {
      if (!this.testAgentId || !this.currentDefinition.collectionCommand) {
        ElMessage.warning('请选择代理并填写采集命令')
        return
      }
      
      this.testExecuting = true
      this.testResult = null
      
      try {
        const response = await fetch(`/api/agents/${this.testAgentId}/execute`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ command: this.currentDefinition.collectionCommand })
        })
        
        if (response.ok) {
          const result = await response.json()
          
          // Validate the result against metric type
          const validation = this.validateCommandResult(result)
          
          this.testResult = {
            ...result,
            valid: validation.valid,
            message: validation.message
          }
          
          if (validation.valid) {
            ElMessage.success('命令测试成功，输出格式验证通过')
          } else {
            ElMessage.error('命令执行成功，但输出格式不符合指标类型要求')
          }
        } else {
          ElMessage.error('命令执行失败')
          this.testResult = {
            exitCode: -1,
            output: '',
            error: `HTTP ${response.status}: ${response.statusText}`,
            valid: false,
            message: '命令执行失败'
          }
        }
      } catch (error) {
        console.error('Error executing test command:', error)
        ElMessage.error('测试失败: ' + error.message)
        this.testResult = {
          exitCode: -1,
          output: '',
          error: error.message,
          valid: false,
          message: '网络错误或代理不可达'
        }
      } finally {
        this.testExecuting = false
      }
    },
    
    validateCommandResult(result) {
      // Check if command executed successfully
      if (result.exitCode !== 0) {
        return {
          valid: false,
          message: `命令执行失败，退出码: ${result.exitCode}`
        }
      }
      
      const output = result.output ? result.output.trim() : ''
      
      // Empty output is invalid for all types
      if (!output) {
        return {
          valid: false,
          message: '命令输出为空，无法采集指标值'
        }
      }
      
      const metricType = this.currentDefinition.metricType
      
      switch (metricType) {
        case 'NUMERIC':
          return this.validateNumericOutput(output)
        case 'BOOLEAN':
          return this.validateBooleanOutput(output)
        case 'STRING':
          return this.validateStringOutput(output)
        default:
          return {
            valid: false,
            message: `未知的指标类型: ${metricType}`
          }
      }
    },
    
    validateNumericOutput(output) {
      // Try to parse as number
      const num = parseFloat(output)
      
      if (isNaN(num)) {
        return {
          valid: false,
          message: `输出 "${output}" 无法解析为数值。数值型指标要求输出纯数字，如: 85.3, 1024, 0.5`
        }
      }
      
      return {
        valid: true,
        message: `✓ 成功解析为数值: ${num}`
      }
    },
    
    validateBooleanOutput(output) {
      // Boolean should be 0, 1, true, false, yes, no
      const normalized = output.toLowerCase()
      
      const validBooleans = ['0', '1', 'true', 'false', 'yes', 'no']
      
      if (!validBooleans.includes(normalized)) {
        return {
          valid: false,
          message: `输出 "${output}" 不是有效的布尔值。布尔型指标要求输出: 0, 1, true, false, yes, no`
        }
      }
      
      const boolValue = normalized === '1' || normalized === 'true' || normalized === 'yes'
      
      return {
        valid: true,
        message: `✓ 成功解析为布尔值: ${boolValue} (原始值: ${output})`
      }
    },
    
    validateStringOutput(output) {
      // String can be anything non-empty
      // But warn if it's too long
      if (output.length > 500) {
        return {
          valid: true,
          message: `✓ 字符串有效，但长度较长 (${output.length} 字符)，建议优化命令输出`
        }
      }
      
      return {
        valid: true,
        message: `✓ 字符串有效 (${output.length} 字符): "${output.substring(0, 50)}${output.length > 50 ? '...' : ''}"`
      }
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

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 16px;
}
</style>
