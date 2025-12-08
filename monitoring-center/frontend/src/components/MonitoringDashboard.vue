<template>
  <div class="monitoring-dashboard">
   
    
    <!-- Summary Cards -->
    <el-card shadow="hover" class="summary-cards-container">
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="summary-card-item">
            <el-statistic title="活跃告警" :value="activeAlertsCount">
              <template #prefix>
                <el-icon color="#f56c6c"><Bell /></el-icon>
              </template>
            </el-statistic>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="summary-card-item">
            <el-statistic title="已关闭告警" :value="resolvedAlertsCount">
              <template #prefix>
                <el-icon color="#67c23a"><CircleCheck /></el-icon>
              </template>
            </el-statistic>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="summary-card-item">
            <el-statistic title="总告警数" :value="totalAlertsCount">
              <template #prefix>
                <el-icon color="#909399"><List /></el-icon>
              </template>
            </el-statistic>
          </div>
        </el-col>
      </el-row>
    </el-card>
    
    <!-- Alert Filters -->
    <el-card shadow="hover" class="filter-card">
      <template #header>
        <div class="card-header">
          <span>告警筛选</span>
        </div>
      </template>
      <el-form :inline="true" label-width="100px">
        <el-form-item label="状态">
          <el-select v-model="alertFilters.status" @change="filterAlerts" placeholder="全部" style="width: 150px">
            <el-option label="全部" value="" />
            <el-option label="打开" value="ACTIVE" />
            <el-option label="已确认" value="ACKNOWLEDGED" />
            <el-option label="已关闭" value="RESOLVED" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="指标类型">
          <el-select v-model="alertFilters.metricType" @change="filterAlerts" placeholder="全部" style="width: 150px">
            <el-option label="全部" value="" />
            <el-option label="CPU使用率" value="CPU" />
            <el-option label="内存使用率" value="MEMORY" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="严重程度">
          <el-select v-model="alertFilters.severity" @change="filterAlerts" placeholder="全部" style="width: 150px">
            <el-option label="全部" value="" />
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
            <el-option label="严重" value="CRITICAL" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="代理IP">
          <el-input v-model="alertFilters.agentIp" @input="filterAlerts" placeholder="搜索代理IP" style="width: 200px" />
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- Active Alerts Table -->
    <el-card shadow="hover" class="alerts-table">
      <template #header>
        <div class="card-header">
          <span>告警列表</span>
          <el-tag v-if="alertFilters.status" type="info" size="small">
            {{ getStatusText(alertFilters.status) }}
          </el-tag>
        </div>
      </template>
      <el-table :data="filteredAlerts" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="指标类型" width="150">
          <template #default="scope">
            <el-tag :type="scope.row.metricType === 'CPU' ? 'danger' : 'primary'" size="small">
              {{ getMetricTypeName(scope.row.metricType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="代理IP" min-width="150">
          <template #default="scope">
            {{ getAgentIp(scope.row.agentId) }}
          </template>
        </el-table-column>
        <el-table-column label="触发值" width="120">
          <template #default="scope">
            <span class="metric-value">{{ formatValue(scope.row.triggerValue) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="threshold" label="阈值" width="100" />
        <el-table-column label="严重程度" width="120">
          <template #default="scope">
            <el-tag :type="getSeverityType(scope.row.severity)" size="small">
              {{ getSeverityText(scope.row.severity) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后触发" min-width="180">
          <template #default="scope">
            {{ formatTime(scope.row.lastTriggeredAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="triggerCount" label="触发次数" width="100" />
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button type="primary" size="small" @click="openEmergencyDrawer(scope.row)">
              应急处理
            </el-button>
            <el-button 
              v-if="scope.row.status === 'ACTIVE' || scope.row.status === 'ACKNOWLEDGED'"
              type="success" 
              size="small" 
              @click="closeAlert(scope.row.id)"
            >
              关闭
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无告警数据" />
        </template>
      </el-table>
    </el-card>
    
    <!-- Emergency Drawer -->
    <el-drawer v-model="isEmergencyDrawerOpen" title="应急处理指南" direction="rtl" size="700px">
      <div v-if="selectedAlert">
        <!-- Alert Details -->
        <el-card class="mb-3" shadow="hover">
          <template #header>
            <span style="font-weight: 600; color: #1976d2;">告警详情</span>
          </template>
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="告警ID">{{ selectedAlert.id }}</el-descriptions-item>
            <el-descriptions-item label="指标类型">
              <el-tag :type="selectedAlert.metricType === 'CPU' ? 'danger' : 'primary'" size="small">
                {{ getMetricTypeName(selectedAlert.metricType) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="代理IP">{{ getAgentIp(selectedAlert.agentId) }}</el-descriptions-item>
            <el-descriptions-item label="触发值">{{ formatValue(selectedAlert.triggerValue) }}</el-descriptions-item>
            <el-descriptions-item label="阈值">{{ selectedAlert.threshold }}</el-descriptions-item>
            <el-descriptions-item label="严重程度">
              <el-tag :type="getSeverityType(selectedAlert.severity)" size="small">
                {{ getSeverityText(selectedAlert.severity) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="首次触发">{{ formatTime(selectedAlert.firstTriggeredAt) }}</el-descriptions-item>
            <el-descriptions-item label="最后触发">{{ formatTime(selectedAlert.lastTriggeredAt) }}</el-descriptions-item>
            <el-descriptions-item label="触发次数" :span="2">{{ selectedAlert.triggerCount }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
        
        <!-- Emergency Steps -->
        <el-card class="mb-3" shadow="hover">
          <template #header>
            <span style="font-weight: 600; color: #1976d2;">应急处理步骤</span>
          </template>
          
          <!-- Emergency Knowledge Base -->
          <div v-if="emergencyKnowledge && emergencyKnowledge.steps && emergencyKnowledge.steps.length > 0">
            <el-alert type="info" :closable="false" style="margin-bottom: 15px;">
              <template #title>
                <strong>{{ emergencyKnowledge.title }}</strong>
              </template>
              <p v-if="emergencyKnowledge.description" style="margin: 5px 0 0 0;">{{ emergencyKnowledge.description }}</p>
            </el-alert>
            
            <el-timeline>
              <el-timeline-item 
                v-for="(step, index) in emergencyKnowledge.steps" 
                :key="step.id"
                :timestamp="`步骤 ${index + 1}`"
                placement="top"
              >
                <el-card shadow="hover">
                  <template #header>
                    <div style="font-weight: 600; color: #1976d2;">{{ step.description }}</div>
                  </template>
                  
                  <div class="command-box">
                    <div class="command-label">执行命令：</div>
                    <pre class="command-text">{{ step.linuxCommand }}</pre>
                    <div style="display: flex; gap: 10px; margin-top: 10px;">
                      <el-button 
                        size="small"
                        type="primary"
                        @click="executeStepCommand(step, index)" 
                        :loading="isExecutingCommand && executingCommand === step.linuxCommand"
                        :disabled="isExecutingCommand"
                      >
                        {{ isExecutingCommand && executingCommand === step.linuxCommand ? '执行中' : '执行' }}
                      </el-button>
                      <el-button 
                        size="small"
                        @click="copyCommand(step.linuxCommand)"
                      >
                        复制命令
                      </el-button>
                    </div>
                  </div>
                  
                  <el-alert v-if="step.agentId" type="success" :closable="false" style="margin-top: 10px;">
                    执行代理: {{ getAgentName(step.agentId) }}
                  </el-alert>
                  <el-alert v-if="step.dependsOn" type="warning" :closable="false" style="margin-top: 10px;">
                    依赖步骤: 步骤 {{ findStepNumber(step.dependsOn) }}
                  </el-alert>
                  <el-alert v-if="step.notes" type="info" :closable="false" style="margin-top: 10px;">
                    {{ step.notes }}
                  </el-alert>
                  
                  <!-- Command Result Display -->
                  <el-card v-if="stepResults[index]" class="mt-2" shadow="never">
                    <template #header>
                      <div style="display: flex; justify-content: space-between; align-items: center;">
                        <span style="font-weight: 600; color: #1976d2;">执行结果</span>
                        <el-button size="small" text @click="clearStepResult(index)">清除</el-button>
                      </div>
                    </template>
                    <el-descriptions :column="2" size="small">
                      <el-descriptions-item label="退出码">
                        <el-tag :type="stepResults[index].exitCode === 0 ? 'success' : 'danger'" size="small">
                          {{ stepResults[index].exitCode }}
                        </el-tag>
                      </el-descriptions-item>
                      <el-descriptions-item label="执行时间">
                        {{ formatTime(stepResults[index].timestamp) }}
                      </el-descriptions-item>
                    </el-descriptions>
                    <el-tabs v-model="stepResultTab[index]" style="margin-top: 10px;">
                      <el-tab-pane label="标准输出" name="output">
                        <pre v-if="stepResults[index].output" class="output-text">{{ stepResults[index].output }}</pre>
                        <el-empty v-else description="无标准输出" :image-size="60" />
                      </el-tab-pane>
                      <el-tab-pane label="错误输出" name="error">
                        <pre v-if="stepResults[index].error" class="error-text">{{ stepResults[index].error }}</pre>
                        <el-empty v-else description="无错误输出" :image-size="60" />
                      </el-tab-pane>
                    </el-tabs>
                  </el-card>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
          
          <!-- Default Steps (if no knowledge base) -->
          <div v-else>
            <el-alert type="warning" :closable="false" style="margin-bottom: 15px;">
              暂无该告警规则的应急知识库，请在应急知识库页面配置。以下为默认应急流程：
            </el-alert>
          </div>
        </el-card>
        
        <!-- Metric Visualization Section -->
        <el-card class="mb-3" shadow="hover">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span style="font-weight: 600; color: #1976d2;">监控指标视图</span>
              <el-radio-group v-model="metricViewMode" size="small">
                <el-radio-button label="chart">图表视图</el-radio-button>
                <el-radio-button label="table">表格视图</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          
          <!-- Chart View -->
          <div v-if="metricViewMode === 'chart'" style="height:300px">
            <div style="display:flex;justify-content:flex-end;margin-bottom:10px">
              <el-button size="small" @click="refreshChart">刷新图表</el-button>
            </div>
            <canvas ref="chartCanvasRef" style="width:100%;height:calc(100% - 40px)"></canvas>
          </div>
          
          <!-- Table View -->
          <div v-if="metricViewMode === 'table'">
            <el-table :data="paginatedMetrics" stripe style="width: 100%" size="small">
              <el-table-column label="时间" min-width="180">
                <template #default="scope">
                  {{ formatTime(scope.row.timestamp) }}
                </template>
              </el-table-column>
              <el-table-column label="数值" width="120">
                <template #default="scope">
                  <span class="metric-value">{{ formatValue(scope.row.value) }}</span>
                </template>
              </el-table-column>
              <template #empty>
                <el-empty description="暂无数据" :image-size="60" />
              </template>
            </el-table>
            <div style="margin-top: 15px; display: flex; justify-content: center;">
              <el-pagination
                v-model:current-page="currentPage"
                :page-size="itemsPerPage"
                :total="totalMetrics"
                layout="prev, pager, next, total"
                small
                @current-change="handlePageChange"
              />
            </div>
          </div>
        </el-card>
        
        <!-- Command Execution Section -->
        <el-card class="mb-3" shadow="hover">
          <template #header>
            <div @click="showCommandWindow = !showCommandWindow" style="cursor: pointer; display: flex; align-items: center; gap: 8px;">
              <el-icon>
                <component :is="showCommandWindow ? 'ArrowDown' : 'ArrowRight'" />
              </el-icon>
              <span style="font-weight: 600; color: #1976d2;">命令执行窗口</span>
            </div>
          </template>
          
          <el-collapse-transition>
            <div v-show="showCommandWindow">
              <el-form label-width="100px">
                <el-form-item label="目标代理">
                  <el-input v-model="commandForm.agentInfo" disabled />
                </el-form-item>
                <el-form-item label="执行命令">
                  <el-input
                    v-model="commandForm.command"
                    type="textarea"
                    :rows="3"
                    placeholder="输入要执行的Linux命令，例如: ps aux | grep java"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button 
                    type="primary"
                    @click="executeCommand" 
                    :disabled="!commandForm.command.trim() || isExecutingCommand"
                    :loading="isExecutingCommand"
                  >
                    {{ isExecutingCommand ? '执行中' : '执行命令' }}
                  </el-button>
                  <el-button @click="clearCommandResult" :disabled="!commandResult">
                    清空结果
                  </el-button>
                </el-form-item>
              </el-form>
              
              <!-- Command Result Display -->
              <el-card v-if="commandResult" shadow="never" style="margin-top: 15px;">
                <template #header>
                  <span style="font-weight: 600; color: #1976d2;">执行结果</span>
                </template>
                <el-descriptions :column="2" size="small" border>
                  <el-descriptions-item label="命令" :span="2">{{ commandResult.command }}</el-descriptions-item>
                  <el-descriptions-item label="退出码">
                    <el-tag :type="commandResult.exitCode === 0 ? 'success' : 'danger'" size="small">
                      {{ commandResult.exitCode }}
                    </el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="执行时间">{{ formatTime(commandResult.timestamp) }}</el-descriptions-item>
                </el-descriptions>
                
                <el-tabs v-model="activeTab" style="margin-top: 15px;">
                  <el-tab-pane label="标准输出" name="output">
                    <pre v-if="commandResult.output" class="output-text">{{ commandResult.output }}</pre>
                    <el-empty v-else description="无标准输出" :image-size="60" />
                  </el-tab-pane>
                  <el-tab-pane label="错误输出" name="error">
                    <pre v-if="commandResult.error" class="error-text">{{ commandResult.error }}</pre>
                    <el-empty v-else description="无错误输出" :image-size="60" />
                  </el-tab-pane>
                </el-tabs>
              </el-card>
            </div>
          </el-collapse-transition>
        </el-card>
        
        <!-- Contact Info -->
        <el-card shadow="hover">
          <template #header>
            <span style="font-weight: 600; color: #1976d2;">紧急联系方式</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="系统管理员">张三 - 138****1234</el-descriptions-item>
            <el-descriptions-item label="网络管理员">李四 - 139****5678</el-descriptions-item>
            <el-descriptions-item label="值班电话">010-12345678</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { DataBoard, Bell, ArrowDown, ArrowRight, CircleCheck, List } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { Chart } from 'chart.js/auto'
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'

export default {
  name: 'MonitoringDashboard',
  components: {
    DataBoard,
    Bell,
    ArrowDown,
    ArrowRight,
    CircleCheck,
    List
  },
  setup() {
    const alerts = ref([])
    const agents = ref([])
    const loading = ref(false)
    const refreshInterval = ref(null)
    const alertFilters = ref({
      agentIp: '',
      severity: '',
      metricType: '',
      status: 'ACTIVE' // Default to show ACTIVE alerts
    })
    const isEmergencyDrawerOpen = ref(false)
    const selectedAlert = ref(null)
    const metricViewMode = ref('chart')
    const chartInstance = ref(null)
    const metricsForSelectedAlert = ref([])
    const currentPage = ref(1)
    const itemsPerPage = ref(10)
    const totalMetrics = ref(0)
    const commandForm = ref({
      command: '',
      agentInfo: ''
    })
    const commandResult = ref(null)
    const isExecutingCommand = ref(false)
    const maxDisplayPoints = ref(50) // Maximum points to display in emergency chart
    const activeTab = ref('output') // For tab switching in command result
    const chartCanvasRef = ref(null) // Ref for the chart canvas element
    const emergencyKnowledge = ref(null) // Emergency knowledge base for the alert rule
    const showCommandWindow = ref(false) // Command window collapsed by default
    const executingCommand = ref('') // Track which command is being executed
    const stepResults = ref({}) // Store results for each step
    const stepResultTab = ref({}) // Track active tab for each step result
    
    // Computed properties
    const activeAlertsCount = computed(() => {
      return alerts.value.filter(alert => alert.status === 'ACTIVE').length
    })
    
    const resolvedAlertsCount = computed(() => {
      return alerts.value.filter(alert => alert.status === 'RESOLVED').length
    })
    
    const totalAlertsCount = computed(() => {
      return alerts.value.length
    })
    
    const filteredAlerts = computed(() => {
      // Apply all filters
      let filtered = alerts.value
      
      // Filter by status
      if (alertFilters.value.status) {
        filtered = filtered.filter(alert => alert.status === alertFilters.value.status)
      }
      
      // Filter by metric type
      if (alertFilters.value.metricType) {
        filtered = filtered.filter(alert => alert.metricType === alertFilters.value.metricType)
      }
      
      // Filter by severity
      if (alertFilters.value.severity) {
        filtered = filtered.filter(alert => alert.severity === alertFilters.value.severity)
      }
      
      // Filter by agent IP
      if (alertFilters.value.agentIp) {
        filtered = filtered.filter(alert => {
          const agent = agents.value.find(a => a.id === alert.agentId)
          return agent && agent.ip.includes(alertFilters.value.agentIp)
        })
      }
      
      // Sort by last triggered time (newest first)
      return filtered.sort((a, b) => b.lastTriggeredAt - a.lastTriggeredAt)
    })
    
    const paginatedMetrics = computed(() => {
      const startIndex = (currentPage.value - 1) * itemsPerPage.value
      const endIndex = startIndex + itemsPerPage.value
      return metricsForSelectedAlert.value.slice(startIndex, endIndex)
    })
    
    // Watchers
    watch(metricViewMode, () => {
      if (metricViewMode.value === 'chart' && selectedAlert.value) {
        // Add a small delay to ensure the canvas is ready
        setTimeout(() => {
          renderChart()
        }, 50)
      } else if (metricViewMode.value === 'table') {
        // Destroy chart when switching to table view
        if (chartInstance.value) {
          chartInstance.value.destroy()
          chartInstance.value = null
        }
        
        // Clear the canvas
        if (chartCanvasRef.value) {
          const ctx = chartCanvasRef.value.getContext('2d')
          if (ctx) {
            ctx.clearRect(0, 0, chartCanvasRef.value.width, chartCanvasRef.value.height)
          }
        }
      }
    })
    
    watch(selectedAlert, (newVal) => {
      if (newVal) {
        // Use nextTick to ensure the DOM is updated before loading data
        nextTick(() => {
          loadMetricsForAlert()
          commandForm.value.agentInfo = getAgentIp(newVal.agentId)
          commandForm.value.command = ''
          commandResult.value = null
          activeTab.value = 'output' // Reset to output tab when selecting new alert
        })
      }
    })

    // Add a watcher for metrics data
    watch(metricsForSelectedAlert, (newVal) => {
      if (metricViewMode.value === 'chart' && newVal && newVal.length > 0) {
        // Add a small delay to ensure the canvas is ready
        setTimeout(() => {
          renderChart()
        }, 50)
      }
    })
    
    watch(isEmergencyDrawerOpen, (isOpen) => {
      if (!isOpen) {
        // Drawer is closing, destroy the chart
        if (chartInstance.value) {
          chartInstance.value.destroy()
          chartInstance.value = null
        }
        
        // Clear the canvas
        if (chartCanvasRef.value) {
          const ctx = chartCanvasRef.value.getContext('2d')
          if (ctx) {
            ctx.clearRect(0, 0, chartCanvasRef.value.width, chartCanvasRef.value.height)
          }
        }
      }
    })
    
    // Lifecycle hooks
    onMounted(() => {
      loadData()
      // Remove auto-refresh
      // refreshInterval.value = setInterval(loadData, 30000)
      
      // Add resize listener
      window.addEventListener('resize', handleResize)
    })

    onBeforeUnmount(() => {
      if (refreshInterval.value) {
        clearInterval(refreshInterval.value)
      }
      if (chartInstance.value) {
        chartInstance.value.destroy()
      }
      
      // Remove resize listener
      window.removeEventListener('resize', handleResize)
    })

    // Resize handler for chart
    const handleResize = () => {
      if (chartInstance.value) {
        chartInstance.value.resize()
      }
    }
    
    // Methods
    const loadData = async () => {
      loading.value = true
      try {
        // Load all alerts (filtering will be done in the frontend)
        const alertsResponse = await fetch('/api/alerts')
        alerts.value = await alertsResponse.json()
        
        // Load agents to get IP addresses
        const agentsResponse = await fetch('/api/agents')
        agents.value = await agentsResponse.json()
      } catch (error) {
        console.error('Error loading dashboard data:', error)
        ElMessage.error('加载数据失败')
      } finally {
        loading.value = false
      }
    }
    
    const filterAlerts = () => {
      // Filtering is handled by the computed property filteredAlerts
      // This method is called on input changes to trigger reactivity
    }
    
    const loadMetricsForAlert = async () => {
      if (!selectedAlert.value) return
      
      try {
        // Reset pagination
        currentPage.value = 1
        
        // Load metrics for the selected alert's agent and metric type with pagination
        const page = currentPage.value - 1 // Backend uses 0-based indexing
        const size = itemsPerPage.value
        
        const metricsResponse = await fetch(
          `/api/metrics/agent/${selectedAlert.value.agentId}/type/${selectedAlert.value.metricType}?page=${page}&size=${size}`
        )
        const data = await metricsResponse.json()
        
        // Handle paginated response
        if (data.content) {
          metricsForSelectedAlert.value = data.content
          totalMetrics.value = data.totalElements
        } else {
          // Fallback to non-paginated if needed
          metricsForSelectedAlert.value = data
          totalMetrics.value = data.length
        }
        
        // Sort by timestamp descending (newest first)
        metricsForSelectedAlert.value.sort((a, b) => b.timestamp - a.timestamp)
        
        // Render chart if in chart view mode
        if (metricViewMode.value === 'chart') {
          // Add a small delay to ensure the canvas is ready
          setTimeout(() => {
            renderChart()
          }, 50)
        }
      } catch (error) {
        console.error('Error loading metrics for alert:', error)
        metricsForSelectedAlert.value = []
        totalMetrics.value = 0
      }
    }
    
    const handlePageChange = (newPage) => {
      currentPage.value = newPage
      loadMetricsForAlert()
    }
    
    const handleSizeChange = (newSize) => {
      itemsPerPage.value = newSize
      currentPage.value = 1 // Reset to first page
      loadMetricsForAlert()
    }
    
    const renderChart = () => {
      // Use nextTick to ensure DOM is updated before accessing the element
      nextTick(() => {
        const chartElement = chartCanvasRef.value
        if (!chartElement || !metricsForSelectedAlert.value.length) {
          // Clear the canvas if no data
          if (chartElement) {
            const ctx = chartElement.getContext('2d')
            if (ctx) {
              ctx.clearRect(0, 0, chartElement.width, chartElement.height)
              ctx.fillStyle = '#9e9e9e'
              ctx.font = '16px Arial'
              ctx.textAlign = 'center'
              ctx.fillText('暂无数据', chartElement.width / 2, chartElement.height / 2)
            }
          }
          return
        }
        
        // Check if the canvas is visible (part of the DOM and has dimensions)
        if (chartElement.offsetWidth === 0 || chartElement.offsetHeight === 0) {
          console.warn('Chart canvas is not visible or has no dimensions')
          return
        }
        
        // Destroy existing chart
        if (chartInstance.value) {
          chartInstance.value.destroy()
        }
        
        try {
          const ctx = chartElement.getContext('2d')
          if (!ctx) {
            console.error('Failed to get 2D context for chart')
            return
          }
          
          // Clear previous content
          ctx.clearRect(0, 0, chartElement.width, chartElement.height)
          
          // Prepare data for chart - limit to maxDisplayPoints for performance
          const chartData = prepareChartData()
          
          chartInstance.value = new Chart(ctx, {
            type: 'line',
            data: chartData,
            options: {
              responsive: true,
              maintainAspectRatio: false,
              plugins: {
                title: {
                  display: true,
                  text: `${getAgentIp(selectedAlert.value.agentId)} - ${getMetricTypeName(selectedAlert.value.metricType)} 趋势`,
                  font: {
                    size: 16,
                    weight: 'bold'
                  },
                  color: '#1976d2'
                },
                legend: {
                  display: true,
                  position: 'top'
                },
                tooltip: {
                  mode: 'index',
                  intersect: false
                }
              },
              scales: {
                y: {
                  beginAtZero: true,
                  max: 100,
                  ticks: {
                    callback: function(value) {
                      return value + '%'
                    },
                    color: '#000000',
                    font: {
                      weight: '600'
                    }
                  },
                  grid: {
                    color: 'rgba(0, 0, 0, 0.1)'
                  }
                },
                x: {
                  ticks: {
                    color: '#000000',
                    font: {
                      weight: '600'
                    },
                    maxTicksLimit: 10, // Limit number of x-axis labels
                    maxRotation: 45,
                    minRotation: 45
                  },
                  grid: {
                    color: 'rgba(0, 0, 0, 0.1)'
                  }
                }
              }
            }
          })
        } catch (error) {
          console.error('Error rendering chart:', error)
          // Display error message to user
          if (chartElement) {
            const ctx = chartElement.getContext('2d')
            if (ctx) {
              ctx.clearRect(0, 0, chartElement.width, chartElement.height)
              ctx.fillStyle = '#000000'
              ctx.font = '16px Arial'
              ctx.textAlign = 'center'
              ctx.fillText('图表渲染失败，请刷新页面重试', chartElement.width / 2, chartElement.height / 2)
            }
          }
        }
      })
    }
    
    const prepareChartData = () => {
      // Check if we have data and a selected alert
      if (!metricsForSelectedAlert.value.length || !selectedAlert.value) {
        return {
          labels: [],
          datasets: []
        }
      }
      
      // Use all loaded metrics for chart instead of limiting to display points initially
      let data = metricsForSelectedAlert.value.slice(0, Math.min(metricsForSelectedAlert.value.length, maxDisplayPoints.value * 2)).reverse()
      
      // Sample data if there are too many points
      if (data.length > maxDisplayPoints.value) {
        const step = Math.ceil(data.length / maxDisplayPoints.value)
        data = data.filter((_, index) => index % step === 0)
      }
      
      const labels = data.map(m => formatTimeShort(m.timestamp))
      const values = data.map(m => m.value)
      
      return {
        labels: labels,
        datasets: [{
          label: `${getMetricTypeName(selectedAlert.value.metricType)} 趋势`,
          data: values,
          borderColor: selectedAlert.value.metricType === 'CPU' ? '#c62828' : '#1976d2',
          backgroundColor: selectedAlert.value.metricType === 'CPU' ? 'rgba(198, 40, 40, 0.1)' : 'rgba(25, 118, 210, 0.1)',
          borderWidth: 2,
          tension: 0.4,
          pointRadius: data.length > 30 ? 0 : 2, // Hide points when too many data points
          pointHoverRadius: 4
        }]
      }
    }
    
    const getAgentIp = (agentId) => {
      const agent = agents.value.find(a => a.id === agentId)
      return agent ? agent.ip : 'Unknown'
    }
    
    const getAgentName = (agentId) => {
      const agent = agents.value.find(a => a.id === agentId)
      return agent ? `${agent.name} (${agent.ip})` : '未知代理'
    }
    
    const getMetricTypeName = (type) => {
      const typeMap = {
        'CPU': 'CPU使用率',
        'MEMORY': '内存使用率'
      }
      return typeMap[type] || type
    }
    
    const getSeverityText = (severity) => {
      const severityMap = {
        'LOW': '低',
        'MEDIUM': '中',
        'HIGH': '高',
        'CRITICAL': '严重'
      }
      return severityMap[severity] || severity
    }
    
    const getSeverityType = (severity) => {
      const typeMap = {
        'LOW': 'success',
        'MEDIUM': 'warning',
        'HIGH': 'warning',
        'CRITICAL': 'danger'
      }
      return typeMap[severity] || 'info'
    }
    
    const getStatusText = (status) => {
      const statusMap = {
        'ACTIVE': '打开',
        'ACKNOWLEDGED': '已确认',
        'RESOLVED': '已关闭'
      }
      return statusMap[status] || status
    }
    
    const getStatusType = (status) => {
      const typeMap = {
        'ACTIVE': 'danger',
        'ACKNOWLEDGED': 'warning',
        'RESOLVED': 'success'
      }
      return typeMap[status] || 'info'
    }
    
    const formatValue = (value) => {
      return value.toFixed(2) + '%'
    }
    
    const formatTime = (timestamp) => {
      if (!timestamp) return '-'
      return new Date(timestamp).toLocaleString('zh-CN')
    }
    
    const formatTimeShort = (timestamp) => {
      const date = new Date(timestamp)
      return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    }
    
    const openEmergencyDrawer = async (alert) => {
      selectedAlert.value = alert
      metricViewMode.value = 'chart' // Default to chart view
      isEmergencyDrawerOpen.value = true
      
      // Load emergency knowledge for this alert's rule
      try {
        const response = await fetch(`/api/emergency/alert-rule/${alert.alertRuleId}`)
        if (response.ok) {
          emergencyKnowledge.value = await response.json()
        } else {
          emergencyKnowledge.value = null
        }
      } catch (error) {
        console.error('Error loading emergency knowledge:', error)
        emergencyKnowledge.value = null
      }
      
      // Add a small delay to ensure the drawer is fully opened before loading data
      setTimeout(() => {
        loadMetricsForAlert()
      }, 100)
    }
    
    const closeEmergencyDrawer = () => {
      isEmergencyDrawerOpen.value = false
      selectedAlert.value = null
      metricsForSelectedAlert.value = []
      currentPage.value = 1
      totalMetrics.value = 0
      activeTab.value = 'output' // Reset tab
      
      // Clean up chart
      if (chartInstance.value) {
        chartInstance.value.destroy()
        chartInstance.value = null
      }
      
      // Clear canvas ref
      if (chartCanvasRef.value) {
        const ctx = chartCanvasRef.value.getContext('2d')
        ctx.clearRect(0, 0, chartCanvasRef.value.width, chartCanvasRef.value.height)
      }
    }
    
    const executeCommand = async () => {
      if (!selectedAlert.value || !commandForm.value.command.trim()) return
      
      isExecutingCommand.value = true
      commandResult.value = null
      
      try {
        const response = await fetch(`/api/agents/${selectedAlert.value.agentId}/execute`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ command: commandForm.value.command })
        })
        
        if (response.ok) {
          commandResult.value = await response.json()
        } else {
          commandResult.value = {
            error: `HTTP ${response.status}: ${response.statusText}`,
            timestamp: Date.now()
          }
        }
      } catch (error) {
        commandResult.value = {
          error: `网络错误: ${error.message}`,
          timestamp: Date.now()
        }
      } finally {
        isExecutingCommand.value = false
      }
    }
    
    const clearCommandResult = () => {
      commandResult.value = null
    }
    
    const refreshChart = () => {
      // Force a chart re-render
      if (chartInstance.value) {
        chartInstance.value.destroy()
        chartInstance.value = null
      }
      
      // Clear the canvas
      if (chartCanvasRef.value) {
        const ctx = chartCanvasRef.value.getContext('2d')
        if (ctx) {
          ctx.clearRect(0, 0, chartCanvasRef.value.width, chartCanvasRef.value.height)
        }
      }
      
      // Re-render the chart
      renderChart()
    }
    
    const findStepNumber = (stepId) => {
      if (!emergencyKnowledge.value || !emergencyKnowledge.value.steps) return 0
      const index = emergencyKnowledge.value.steps.findIndex(s => s.id === stepId)
      return index >= 0 ? index + 1 : 0
    }
    
    const executeStepCommand = async (step, stepIndex) => {
      if (!selectedAlert.value || !step.linuxCommand.trim()) return
      
      // Determine which agent to execute on
      const targetAgentId = step.agentId || selectedAlert.value.agentId
      
      executingCommand.value = step.linuxCommand
      isExecutingCommand.value = true
      
      try {
        const response = await fetch(`/api/agents/${targetAgentId}/execute`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ command: step.linuxCommand })
        })
        
        if (response.ok) {
          const result = await response.json()
          stepResults.value[stepIndex] = result
          stepResultTab.value[stepIndex] = 'output' // Default to output tab
          ElMessage.success('命令执行成功')
        } else {
          stepResults.value[stepIndex] = {
            error: `HTTP ${response.status}: ${response.statusText}`,
            timestamp: Date.now(),
            exitCode: -1
          }
          stepResultTab.value[stepIndex] = 'error'
          ElMessage.error('命令执行失败')
        }
      } catch (error) {
        stepResults.value[stepIndex] = {
          error: `网络错误: ${error.message}`,
          timestamp: Date.now(),
          exitCode: -1
        }
        stepResultTab.value[stepIndex] = 'error'
        ElMessage.error(`网络错误: ${error.message}`)
      } finally {
        isExecutingCommand.value = false
        executingCommand.value = ''
      }
    }
    
    const copyCommand = (command) => {
      if (navigator.clipboard) {
        navigator.clipboard.writeText(command).then(() => {
          // Could show a toast notification here
          console.log('Command copied to clipboard')
        }).catch(err => {
          console.error('Failed to copy:', err)
        })
      } else {
        // Fallback for older browsers
        const textArea = document.createElement('textarea')
        textArea.value = command
        document.body.appendChild(textArea)
        textArea.select()
        document.execCommand('copy')
        document.body.removeChild(textArea)
      }
    }
    
    const clearStepResult = (index) => {
      delete stepResults.value[index]
      delete stepResultTab.value[index]
    }
    
    const closeAlert = async (alertId) => {
      try {
        const response = await fetch(`/api/alerts/${alertId}/resolve`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ resolveNote: '用户手动关闭' })
        })
        
        if (response.ok) {
          ElMessage.success('告警已关闭')
          // Reload alerts to update the list
          await loadData()
        } else {
          ElMessage.error('关闭告警失败')
        }
      } catch (error) {
        console.error('Error closing alert:', error)
        ElMessage.error('关闭告警失败')
      }
    }
    
    return {
      // Reactive data
      alerts,
      agents,
      alertFilters,
      isEmergencyDrawerOpen,
      selectedAlert,
      metricViewMode,
      metricsForSelectedAlert,
      currentPage,
      itemsPerPage,
      totalMetrics,
      commandForm,
      commandResult,
      isExecutingCommand,
      maxDisplayPoints,
      activeTab,
      chartCanvasRef, // Add chartCanvasRef to returned values
      emergencyKnowledge, // Add emergencyKnowledge to returned values
      showCommandWindow, // Add showCommandWindow to returned values
      executingCommand, // Add executingCommand to returned values
      stepResults, // Add stepResults to returned values
      stepResultTab, // Add stepResultTab to returned values
      
      // Computed properties
      activeAlertsCount,
      resolvedAlertsCount,
      totalAlertsCount,
      filteredAlerts,
      paginatedMetrics,
      
      // Methods
      getAgentIp,
      getSeverityType,
      getStatusText,
      getStatusType,
      loading,
      getMetricTypeName,
      getSeverityText,
      getAgentName,
      formatValue,
      formatTime,
      formatTimeShort,
      openEmergencyDrawer,
      closeEmergencyDrawer,
      loadMetricsForAlert,
      handlePageChange,
      handleSizeChange,
      renderChart,
      executeCommand,
      clearCommandResult,
      filterAlerts,
      handleResize, // Add handleResize to returned values
      refreshChart, // Add refreshChart to returned values
      findStepNumber, // Add findStepNumber to returned values
      executeStepCommand, // Add executeStepCommand to returned values
      copyCommand, // Add copyCommand to returned values
      clearStepResult, // Add clearStepResult to returned values
      closeAlert // Add closeAlert to returned values
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

.monitoring-dashboard {
  padding: 20px;
}

.el-page-header {
  margin-bottom: 24px;
}

.summary-cards-container {
  margin-bottom: 20px;
}

.summary-card-item {
  text-align: center;
  padding: 10px 0;
}

.filter-card,
.alerts-table {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 16px;
}

.metric-value {
  font-weight: 600;
  color: #1976d2;
}

.mb-3 {
  margin-bottom: 16px;
}

.mt-2 {
  margin-top: 12px;
}

.command-box {
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 12px;
}

.command-label {
  color: #1976d2;
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 8px;
}

.command-text {
  background: #263238;
  color: #4caf50;
  padding: 10px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.5;
}

.output-text,
.error-text {
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  max-height: 200px;
  overflow-y: auto;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.error-text {
  background: #fff3f3;
  color: #f56c6c;
}

/* General Card Styles */
.section-header h2 {
  color: #1976d2;
  font-size: 24px;
  margin-bottom: 25px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
}

.section-header .icon {
  font-size: 28px;
}

.card {
  background: rgba(255, 255, 255, 0.95);
  border: 2px solid #e3f2fd;
  border-radius: 12px;
  padding: 25px;
  margin-bottom: 25px;
  box-shadow: 0 4px 20px rgba(25, 118, 210, 0.1);
}

.card h3 {
  color: #1976d2;
  font-size: 18px;
  margin-bottom: 20px;
  border-bottom: 2px solid #e3f2fd;
  padding-bottom: 10px;
  font-weight: 600;
}

.mb-3 {
  margin-bottom: 25px;
}

.mt-3 {
  margin-top: 25px;
}

.dashboard-summary {
  display: flex;
  gap: 12px;
  margin-bottom: 25px;
}

.summary-card {
  width: 200px;
  background: rgba(255, 255, 255, 0.95);
  border: 2px solid #e3f2fd;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(25, 118, 210, 0.1);
}

.card-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-icon {
  font-size: 24px;
}

.card-info {
  flex: 1;
}

.card-title {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.card-value {
  font-size: 20px;
  font-weight: bold;
  color: #1976d2;
}

/* Filter section styles */
.filter-section {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  align-items: center;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-group label {
  font-weight: 600;
  color: #1976d2;
  font-size: 14px;
  white-space: nowrap;
}

.filter-select {
  padding: 8px 15px;
  border: 2px solid #1976d2;
  border-radius: 6px;
  background: #ffffff;
  color: #1976d2;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 120px;
}

.filter-select:hover {
  border-color: #0d47a1;
  background: #e3f2fd;
}

.filter-select:focus {
  outline: none;
  border-color: #0d47a1;
  box-shadow: 0 0 0 3px rgba(25, 118, 210, 0.1);
}

.filter-input {
  padding: 8px 15px;
  border: 2px solid #1976d2;
  border-radius: 6px;
  background: #ffffff;
  color: #1976d2;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s ease;
  min-width: 120px;
}

.filter-input:focus {
  outline: none;
  border-color: #0d47a1;
  box-shadow: 0 0 0 3px rgba(25, 118, 210, 0.1);
}

.filter-input::placeholder {
  color: #90caf9;
}

/* Table styles */
.table-container {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

thead {
  background: #e3f2fd;
}

th {
  padding: 15px;
  text-align: left;
  color: #1976d2;
  font-weight: 600;
  font-size: 14px;
  border-bottom: 2px solid #bbdefb;
}

td {
  padding: 15px;
  color: #000000;
  border-bottom: 1px solid #e3f2fd;
}

tbody tr {
  transition: all 0.3s ease;
}

tbody tr:hover {
  background: #f5f5f5;
}

.no-data {
  text-align: center;
  color: #9e9e9e;
  font-style: italic;
  padding: 30px !important;
}

.metric-type {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.metric-cpu {
  background: #ffebee;
  color: #c62828;
  border: 1px solid #ef5350;
}

.metric-memory {
  background: #e3f2fd;
  color: #1976d2;
  border: 1px solid #64b5f6;
}

.metric-value {
  font-weight: 600;
  color: #1976d2;
}

.severity {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.severity-low {
  background: #e8f5e9;
  color: #2e7d32;
  border: 1px solid #66bb6a;
}

.severity-medium {
  background: #fff3e0;
  color: #ef6c00;
  border: 1px solid #ffa726;
}

.severity-high {
  background: #fff3e0;
  color: #f57c00;
  border: 1px solid #ff9800;
}

.severity-critical {
  background: #ffebee;
  color: #c62828;
  border: 1px solid #ef5350;
}

.btn-primary {
  padding: 10px 20px;
  background: #1976d2;
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.btn-primary:hover:not(:disabled) {
  background: #1565c0;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(25, 118, 210, 0.3);
}

.btn-primary:disabled {
  background: #bbdefb;
  cursor: not-allowed;
}

.btn-secondary {
  padding: 10px 20px;
  background: #ffffff;
  border: 2px solid #1976d2;
  border-radius: 6px;
  color: #1976d2;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
  margin-left: 10px;
}

.btn-secondary:hover:not(:disabled) {
  background: #e3f2fd;
  border-color: #0d47a1;
}

.btn-secondary:disabled {
  background: #f5f5f5;
  border-color: #bbdefb;
  color: #90caf9;
  cursor: not-allowed;
}

.btn-loading {
  position: relative;
  pointer-events: none;
}

.btn-loading::after {
  content: "";
  position: absolute;
  width: 16px;
  height: 16px;
  top: 0;
  left: 10px;
  right: 0;
  bottom: 0;
  margin: auto;
  border: 2px solid transparent;
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: button-loading-spinner 1s ease infinite;
}

@keyframes button-loading-spinner {
  from {
    transform: rotate(0turn);
  }
  to {
    transform: rotate(1turn);
  }
}

.output-text, .error-text {
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  padding: 10px;
  font-family: monospace;
  font-size: 12px;
  white-space: pre-wrap;
  max-height: 200px;
  overflow-y: auto;
  margin: 0;
}

.error-text {
  background: #ffebee;
  border-color: #ffcdd2;
  color: #c62828;
}

.no-output {
  text-align: center;
  color: #9e9e9e;
  font-style: italic;
  padding: 20px;
}

.error-code {
  color: #c62828;
  font-weight: bold;
}

.toggle-btn {
  padding: 10px 20px;
  border: 2px solid #1976d2;
  border-radius: 6px;
  background: #ffffff;
  color: #1976d2;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.toggle-btn:hover {
  background: #e3f2fd;
  border-color: #0d47a1;
}

.toggle-btn.active {
  background: #1976d2;
  color: #ffffff;
  border-color: #1976d2;
}

.view-toggle {
  display: flex;
  gap: 10px;
}

/* Form styles */
.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.form-group label {
  font-weight: 600;
  color: #1976d2;
  font-size: 14px;
}

.form-input, .form-textarea {
  padding: 10px 15px;
  border: 2px solid #1976d2;
  border-radius: 6px;
  background: #ffffff;
  color: #1976d2;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.form-input:focus, .form-textarea:focus {
  outline: none;
  border-color: #0d47a1;
  box-shadow: 0 0 0 3px rgba(25, 118, 210, 0.1);
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.form-input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

/* Result styles */
.result-details {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
  margin-top: 15px;
}

.result-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.result-label {
  font-weight: 600;
  color: #1976d2;
  font-size: 13px;
}

.result-value {
  font-weight: 500;
  color: #000000;
  font-size: 14px;
}

/* Tabs styles */
.tabs {
  display: flex;
  border-bottom: 2px solid #e3f2fd;
  margin-bottom: 15px;
}

.tab-btn {
  padding: 10px 20px;
  border: none;
  background: transparent;
  color: #1976d2;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border-bottom: 3px solid transparent;
  transition: all 0.3s ease;
}

.tab-btn:hover {
  background: #e3f2fd;
}

.tab-btn.active {
  border-bottom-color: #1976d2;
  color: #1976d2;
}

.tab-content {
  min-height: 100px;
}

/* Steps styles */
.steps {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.step {
  display: flex;
  gap: 20px;
}

.step-number {
  width: 30px;
  height: 30px;
  background: #1976d2;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  flex-shrink: 0;
}

.step-content {
  flex: 1;
}

.step-content h4 {
  color: #1976d2;
  margin-bottom: 5px;
  font-size: 16px;
}

.step-content p {
  color: #000000;
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
}

/* Contact info styles */
.contact-info {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.contact-item {
  display: flex;
  gap: 10px;
}

.contact-label {
  font-weight: 600;
  color: #1976d2;
  min-width: 100px;
}

.contact-value {
  color: #000000;
  font-weight: 500;
}

/* Metric table styles */
.metric-table-container {
  margin-top: 20px;
}

.table-wrapper {
  overflow-x: auto;
  border: 1px solid #e3f2fd;
  border-radius: 8px;
}

.metric-table {
  width: 100%;
  border-collapse: collapse;
  background: #ffffff;
}

.metric-table thead {
  background: #e3f2fd;
}

.metric-table th {
  padding: 12px 15px;
  text-align: left;
  color: #1976d2;
  font-weight: 600;
  font-size: 14px;
  border-bottom: 2px solid #bbdefb;
}

.metric-table td {
  padding: 12px 15px;
  color: #000000;
  border-bottom: 1px solid #e3f2fd;
}

.metric-table tbody tr {
  transition: all 0.3s ease;
}

.metric-table tbody tr:hover {
  background: #f5f5f5;
}

.metric-table .no-data {
  text-align: center;
  color: #9e9e9e;
  font-style: italic;
  padding: 30px;
}

.metric-value {
  font-weight: 600;
  color: #1976d2;
}

/* Pagination styles */
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px 15px;
  background: #f5f5f5;
  border-radius: 6px;
}

.pagination-btn {
  padding: 8px 15px;
  border: 2px solid #1976d2;
  border-radius: 6px;
  background: #ffffff;
  color: #1976d2;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.pagination-btn:hover:not(:disabled) {
  background: #e3f2fd;
  border-color: #0d47a1;
}

.pagination-btn:disabled {
  background: #f5f5f5;
  border-color: #cccccc;
  color: #999999;
  cursor: not-allowed;
}

.pagination-info {
  font-weight: 600;
  color: #1976d2;
  font-size: 14px;
  white-space: nowrap;
}

/* Emergency Knowledge Styles */
.knowledge-header {
  margin-bottom: 20px;
  padding: 15px;
  background: #e3f2fd;
  border-radius: 8px;
  border-left: 4px solid #1976d2;
}

.knowledge-header h4 {
  color: #1976d2;
  font-size: 18px;
  margin: 0 0 10px 0;
  font-weight: 600;
}

.knowledge-description {
  color: #000000;
  font-size: 14px;
  margin: 0;
  line-height: 1.5;
}

.no-knowledge-hint {
  background: #fff3e0;
  border: 2px solid #ff9800;
  border-radius: 8px;
  padding: 15px;
  color: #000000;
  font-size: 14px;
  margin-bottom: 20px;
  font-weight: 500;
}

.step-command {
  background: #f5f5f5;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  padding: 12px;
  margin-top: 10px;
  position: relative;
}

.command-label {
  color: #1976d2;
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 8px;
}

.command-text {
  background: #263238;
  color: #4caf50;
  padding: 10px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.5;
}

.btn-copy-command {
  margin-top: 10px;
  padding: 6px 12px;
  background: #1976d2;
  border: none;
  border-radius: 4px;
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-copy-command:hover {
  background: #1565c0;
  transform: translateY(-1px);
  box-shadow: 0 3px 10px rgba(25, 118, 210, 0.3);
}

.step-dependency {
  background: #fff3e0;
  border: 1px solid #ff9800;
  border-radius: 6px;
  padding: 8px 12px;
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #e65100;
}

.dependency-icon {
  font-size: 16px;
}

.step-notes {
  background: #e3f2fd;
  border: 1px solid #1976d2;
  border-radius: 6px;
  padding: 8px 12px;
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #000000;
}

.notes-icon {
  font-size: 16px;
}

/* Collapsible Header */
.collapsible-header {
  cursor: pointer;
  user-select: none;
}

.collapsible-header:hover {
  background: #f5f9ff;
}

.collapse-icon {
  display: inline-block;
  margin-right: 8px;
  font-size: 12px;
  transition: transform 0.3s ease;
}

.collapsible-content {
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    max-height: 0;
  }
  to {
    opacity: 1;
    max-height: 2000px;
  }
}

/* Command Actions */
.command-actions {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.btn-execute-command {
  padding: 8px 16px;
  background: #4caf50;
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-execute-command:hover:not(:disabled) {
  background: #43a047;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(76, 175, 80, 0.3);
}

.btn-execute-command:disabled {
  background: #a5d6a7;
  cursor: not-allowed;
}

.btn-copy-command {
  padding: 8px 16px;
  background: #2196f3;
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-copy-command:hover {
  background: #1976d2;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(33, 150, 243, 0.3);
}

/* Step Result Styles */
.step-result {
  background: #f5f5f5;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  padding: 15px;
  margin-top: 15px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.result-title {
  color: #1976d2;
  font-weight: 600;
  font-size: 14px;
}

.btn-clear-result {
  background: #f44336;
  border: none;
  color: white;
  font-size: 20px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.btn-clear-result:hover {
  background: #d32f2f;
  transform: rotate(90deg);
}

.result-summary {
  padding: 8px 12px;
  background: #fff;
  border-radius: 4px;
  margin-bottom: 10px;
  font-size: 13px;
}

.exit-code {
  font-weight: 600;
  color: #4caf50;
}

.exit-code.error-code {
  color: #f44336;
}

.result-time {
  color: #666;
  font-size: 12px;
}

.result-tabs {
  display: flex;
  gap: 5px;
  margin-bottom: 10px;
}

.result-tab {
  padding: 6px 12px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  color: #666;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.result-tab:hover {
  background: #f5f5f5;
}

.result-tab.active {
  background: #1976d2;
  color: #fff;
  border-color: #1976d2;
}

.result-content {
  background: #fff;
  border-radius: 4px;
  padding: 10px;
}

.result-content .output-text,
.result-content .error-text {
  max-height: 150px;
  font-size: 11px;
}
</style>
