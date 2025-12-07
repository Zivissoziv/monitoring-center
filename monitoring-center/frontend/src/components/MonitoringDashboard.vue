<template>
  <div class="container-wide">
    <div class="section-header">
      <h2><span class="icon">📺</span> 监控台</h2>
    </div>
    
    <!-- Summary Cards -->
    <div class="dashboard-summary card">
      <div class="summary-card">
        <div class="card-content">
          <div class="card-icon">🔔</div>
          <div class="card-info">
            <div class="card-title">当前告警</div>
            <div class="card-value">{{ activeAlertsCount }}</div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Alert Filters -->
    <div class="card">
      <h3>告警筛选</h3>
      <div class="filter-section">
        <div class="filter-group">
          <label>指标类型:</label>
          <select v-model="alertFilters.metricType" @change="filterAlerts" class="filter-select">
            <option value="">全部</option>
            <option value="CPU">CPU使用率</option>
            <option value="MEMORY">内存使用率</option>
          </select>
        </div>
        
        <div class="filter-group">
          <label>严重程度:</label>
          <select v-model="alertFilters.severity" @change="filterAlerts" class="filter-select">
            <option value="">全部</option>
            <option value="LOW">低</option>
            <option value="MEDIUM">中</option>
            <option value="HIGH">高</option>
            <option value="CRITICAL">严重</option>
          </select>
        </div>
        
        <div class="filter-group">
          <label>代理IP:</label>
          <input v-model="alertFilters.agentIp" @input="filterAlerts" placeholder="搜索代理IP" class="filter-input" />
        </div>
      </div>
    </div>
    
    <!-- Active Alerts Table -->
    <div class="card">
      <h3>当前告警</h3>
      <div class="table-container">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>指标类型</th>
              <th>代理IP</th>
              <th>触发值</th>
              <th>阈值</th>
              <th>严重程度</th>
              <th>最后触发</th>
              <th>触发次数</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="alert in filteredAlerts" :key="alert.id">
              <td>{{ alert.id }}</td>
              <td><span :class="'metric-type metric-' + alert.metricType.toLowerCase()">{{ getMetricTypeName(alert.metricType) }}</span></td>
              <td>{{ getAgentIp(alert.agentId) }}</td>
              <td><span class="metric-value">{{ formatValue(alert.triggerValue) }}</span></td>
              <td>{{ alert.threshold }}</td>
              <td><span :class="'severity severity-' + alert.severity.toLowerCase()">{{ getSeverityText(alert.severity) }}</span></td>
              <td>{{ formatTime(alert.lastTriggeredAt) }}</td>
              <td>{{ alert.triggerCount }}</td>
              <td>
                <button @click="openEmergencyDrawer(alert)" class="btn-primary">应急处理</button>
              </td>
            </tr>
            <tr v-if="filteredAlerts.length === 0">
              <td colspan="9" class="no-data">暂无告警数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    
    <!-- Emergency Drawer -->
    <el-drawer v-model="isEmergencyDrawerOpen" title="应急处理指南" direction="rtl" size="700px">
      <div v-if="selectedAlert">
        <!-- Alert Details -->
        <div class="card mb-3">
          <h3>告警详情</h3>
          <el-descriptions :column="3" size="small" border>
            <el-descriptions-item label="告警ID">{{ selectedAlert.id }}</el-descriptions-item>
            <el-descriptions-item label="指标类型">{{ getMetricTypeName(selectedAlert.metricType) }}</el-descriptions-item>
            <el-descriptions-item label="代理IP">{{ getAgentIp(selectedAlert.agentId) }}</el-descriptions-item>
            <el-descriptions-item label="触发值">{{ formatValue(selectedAlert.triggerValue) }}</el-descriptions-item>
            <el-descriptions-item label="阈值">{{ selectedAlert.threshold }}</el-descriptions-item>
            <el-descriptions-item label="严重程度">
              <span :class="'severity severity-' + selectedAlert.severity.toLowerCase()">
                {{ getSeverityText(selectedAlert.severity) }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="首次触发">{{ formatTime(selectedAlert.firstTriggeredAt) }}</el-descriptions-item>
            <el-descriptions-item label="最后触发">{{ formatTime(selectedAlert.lastTriggeredAt) }}</el-descriptions-item>
            <el-descriptions-item label="触发次数">{{ selectedAlert.triggerCount }}</el-descriptions-item>
          </el-descriptions>
        </div>
        
        <!-- Emergency Steps (moved to top) -->
        <div class="card mb-3">
          <h3>应急处理步骤</h3>
          
          <!-- Emergency Knowledge Base -->
          <div v-if="emergencyKnowledge && emergencyKnowledge.steps && emergencyKnowledge.steps.length > 0">
            <div class="knowledge-header">
              <h4>{{ emergencyKnowledge.title }}</h4>
              <p v-if="emergencyKnowledge.description" class="knowledge-description">{{ emergencyKnowledge.description }}</p>
            </div>
            
            <div class="steps">
              <div v-for="(step, index) in emergencyKnowledge.steps" :key="step.id" class="step">
                <div class="step-number">{{ index + 1 }}</div>
                <div class="step-content">
                  <h4>{{ step.description }}</h4>
                  <div class="step-command">
                    <div class="command-label">执行命令：</div>
                    <pre class="command-text">{{ step.linuxCommand }}</pre>
                    <div class="command-actions">
                      <button 
                        @click="executeStepCommand(step.linuxCommand)" 
                        :disabled="isExecutingCommand"
                        :class="['btn-execute-command', { 'btn-loading': isExecutingCommand && executingCommand === step.linuxCommand }]"
                        title="直接执行命令"
                      >
                        {{ isExecutingCommand && executingCommand === step.linuxCommand ? '执行中...' : '⚡ 执行' }}
                      </button>
                      <button 
                        @click="copyCommand(step.linuxCommand)" 
                        class="btn-copy-command"
                        title="复制命令"
                      >
                        📋 复制
                      </button>
                    </div>
                  </div>
                  <div v-if="step.dependsOn" class="step-dependency">
                    <span class="dependency-icon">⚠️</span>
                    <span>依赖步骤: 步骤 {{ findStepNumber(step.dependsOn) }}</span>
                  </div>
                  <div v-if="step.notes" class="step-notes">
                    <span class="notes-icon">💡</span>
                    <span>{{ step.notes }}</span>
                  </div>
                  
                  <!-- Command Result Display (per step) -->
                  <div v-if="stepResults[index]" class="step-result mt-2">
                    <div class="result-header">
                      <span class="result-title">执行结果</span>
                      <button @click="clearStepResult(index)" class="btn-clear-result">&times;</button>
                    </div>
                    <div class="result-summary">
                      <span>退出码: </span>
                      <span :class="['exit-code', { 'error-code': stepResults[index].exitCode !== 0 }]">
                        {{ stepResults[index].exitCode }}
                      </span>
                      <span class="result-time"> | {{ formatTime(stepResults[index].timestamp) }}</span>
                    </div>
                    <div class="result-tabs">
                      <button 
                        :class="['result-tab', { active: stepResultTab[index] === 'output' }]" 
                        @click="stepResultTab[index] = 'output'"
                      >
                        标准输出
                      </button>
                      <button 
                        :class="['result-tab', { active: stepResultTab[index] === 'error' }]" 
                        @click="stepResultTab[index] = 'error'"
                      >
                        错误输出
                      </button>
                    </div>
                    <div class="result-content">
                      <div v-if="stepResultTab[index] === 'output'">
                        <pre v-if="stepResults[index].output" class="output-text">{{ stepResults[index].output }}</pre>
                        <div v-else class="no-output">无标准输出</div>
                      </div>
                      <div v-if="stepResultTab[index] === 'error'">
                        <pre v-if="stepResults[index].error" class="error-text">{{ stepResults[index].error }}</pre>
                        <div v-else class="no-output">无错误输出</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- Default Steps (if no knowledge base) -->
          <div v-else>
            <p class="no-knowledge-hint">暂无该告警规则的应急知识库，请在应急知识库页面配置。以下为默认应急流程：</p>
            <div class="steps">
              <div class="step">
                <div class="step-number">1</div>
                <div class="step-content">
                  <h4>确认告警</h4>
                  <p>检查当前告警详情，确认告警的真实性和严重程度</p>
                </div>
              </div>
              <div class="step">
                <div class="step-number">2</div>
                <div class="step-content">
                  <h4>定位问题</h4>
                  <p>根据告警类型和代理IP，登录对应服务器进行问题排查</p>
                </div>
              </div>
              <div class="step">
                <div class="step-number">3</div>
                <div class="step-content">
                  <h4>临时处理</h4>
                  <div v-if="selectedAlert && selectedAlert.metricType === 'CPU'">
                    <p><strong>CPU告警</strong>: 检查高CPU进程，必要时重启相关服务</p>
                  </div>
                  <div v-else-if="selectedAlert && selectedAlert.metricType === 'MEMORY'">
                    <p><strong>内存告警</strong>: 检查内存使用情况，清理缓存或重启应用</p>
                  </div>
                  <div v-else>
                    <p><strong>通用处理</strong>: 检查系统资源使用情况，查看应用日志，必要时重启服务</p>
                  </div>
                </div>
              </div>
              <div class="step">
                <div class="step-number">4</div>
                <div class="step-content">
                  <h4>根因分析</h4>
                  <p>分析系统日志，找出问题根本原因</p>
                </div>
              </div>
              <div class="step">
                <div class="step-number">5</div>
                <div class="step-content">
                  <h4>永久解决</h4>
                  <p>根据根因分析结果，实施永久性解决方案</p>
                </div>
              </div>
              <div class="step">
                <div class="step-number">6</div>
                <div class="step-content">
                  <h4>验证恢复</h4>
                  <p>确认系统恢复正常，监控指标回到正常范围</p>
                </div>
              </div>
              <div class="step">
                <div class="step-number">7</div>
                <div class="step-content">
                  <h4>记录总结</h4>
                  <p>记录问题处理过程和解决方案，更新知识库</p>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Metric Visualization Section -->
        <div class="card mb-3">
          <h3>监控指标视图</h3>
          <div class="toolbar mb-3">
            <div class="view-toggle">
              <button :class="['toggle-btn', { active: metricViewMode === 'chart' }]" @click="metricViewMode = 'chart'">
                📊 图表视图
              </button>
              <button :class="['toggle-btn', { active: metricViewMode === 'table' }]" @click="metricViewMode = 'table'">
                📋 表格视图
              </button>
            </div>
          </div>
          
          <!-- Chart View -->
          <div v-if="metricViewMode === 'chart'" style="height:300px">
            <div style="display:flex;justify-content:flex-end;margin-bottom:10px">
              <button @click="refreshChart" class="btn-secondary" style="padding:5px 10px;font-size:12px">
                刷新图表
              </button>
            </div>
            <canvas ref="chartCanvasRef" style="width:100%;height:calc(100% - 40px)"></canvas>
          </div>
          
          <!-- Table View -->
          <div v-if="metricViewMode === 'table'" class="metric-table-container">
            <div class="table-wrapper">
              <table class="metric-table">
                <thead>
                  <tr>
                    <th>时间</th>
                    <th>数值</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="metric in paginatedMetrics" :key="metric.id">
                    <td>{{ formatTime(metric.timestamp) }}</td>
                    <td><span class="metric-value">{{ formatValue(metric.value) }}</span></td>
                  </tr>
                  <tr v-if="paginatedMetrics.length === 0">
                    <td colspan="2" class="no-data">暂无数据</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="pagination-wrapper">
              <div class="pagination-controls">
                <button 
                  :disabled="currentPage === 1" 
                  @click="handlePageChange(currentPage - 1)"
                  class="pagination-btn"
                >
                  上一页
                </button>
                <span class="pagination-info">
                  第 {{ currentPage }} 页，共 {{ Math.ceil(totalMetrics / itemsPerPage) }} 页
                </span>
                <button 
                  :disabled="currentPage >= Math.ceil(totalMetrics / itemsPerPage)" 
                  @click="handlePageChange(currentPage + 1)"
                  class="pagination-btn"
                >
                  下一页
                </button>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Command Execution Section (Collapsible) -->
        <div class="card mb-3">
          <div class="collapsible-header" @click="showCommandWindow = !showCommandWindow">
            <h3 style="margin: 0; cursor: pointer;">
              <span class="collapse-icon">{{ showCommandWindow ? '▼' : '▶' }}</span>
              命令执行窗口
            </h3>
          </div>
          
          <div v-show="showCommandWindow" class="collapsible-content">
            <div class="form-group">
              <label>目标代理:</label>
              <input v-model="commandForm.agentInfo" disabled class="form-input" />
            </div>
            <div class="form-group">
              <label>执行命令:</label>
              <textarea
                v-model="commandForm.command"
                placeholder="输入要执行的Linux命令，例如: ps aux | grep java"
                class="form-textarea"
                rows="3"
              ></textarea>
            </div>
            <div class="form-actions">
              <button 
                @click="executeCommand" 
                :disabled="!commandForm.command.trim() || isExecutingCommand"
                :class="['btn-primary', { 'btn-loading': isExecutingCommand }]"
              >
                {{ isExecutingCommand ? '执行中...' : '执行命令' }}
              </button>
              <button @click="clearCommandResult" :disabled="!commandResult" class="btn-secondary">
                清空结果
              </button>
            </div>
            
            <!-- Command Result Display -->
            <div v-if="commandResult" class="mt-3">
              <div class="card">
                <h3>执行结果</h3>
                <div class="result-details">
                  <div class="result-item">
                    <span class="result-label">命令:</span>
                    <span class="result-value">{{ commandResult.command }}</span>
                  </div>
                  <div class="result-item">
                    <span class="result-label">退出码:</span>
                    <span :class="['result-value', { 'error-code': commandResult.exitCode !== 0 }]">
                      {{ commandResult.exitCode }}
                    </span>
                  </div>
                  <div class="result-item">
                    <span class="result-label">执行时间:</span>
                    <span class="result-value">{{ formatTime(commandResult.timestamp) }}</span>
                  </div>
                </div>
                
                <div class="mt-3">
                  <div class="tabs">
                    <button 
                      :class="['tab-btn', { active: activeTab === 'output' }]" 
                      @click="activeTab = 'output'"
                    >
                      标准输出
                    </button>
                    <button 
                      :class="['tab-btn', { active: activeTab === 'error' }]" 
                      @click="activeTab = 'error'"
                    >
                      错误输出
                    </button>
                  </div>
                  
                  <div class="tab-content">
                    <div v-if="activeTab === 'output'">
                      <pre v-if="commandResult.output" class="output-text">{{ commandResult.output }}</pre>
                      <div v-else class="no-output">无标准输出</div>
                    </div>
                    <div v-if="activeTab === 'error'">
                      <pre v-if="commandResult.error" class="error-text">{{ commandResult.error }}</pre>
                      <div v-else class="no-output">无错误输出</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Contact Info -->
        <div class="card">
          <h3>紧急联系方式</h3>
          <div class="contact-info">
            <div class="contact-item">
              <span class="contact-label">系统管理员:</span>
              <span class="contact-value">张三 - 138****1234</span>
            </div>
            <div class="contact-item">
              <span class="contact-label">网络管理员:</span>
              <span class="contact-value">李四 - 139****5678</span>
            </div>
            <div class="contact-item">
              <span class="contact-label">值班电话:</span>
              <span class="contact-value">010-12345678</span>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { Chart } from 'chart.js/auto'
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'

export default {
  name: 'MonitoringDashboard',
  setup() {
    const alerts = ref([])
    const agents = ref([])
    const refreshInterval = ref(null)
    const alertFilters = ref({
      agentIp: '',
      severity: '',
      metricType: ''
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
      return filteredAlerts.value.length
    })
    
    const filteredAlerts = computed(() => {
      // Apply all filters
      let filtered = alerts.value.filter(alert => alert.status === 'ACTIVE')
      
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
      try {
        // Load active alerts only
        const alertsResponse = await fetch('/api/alerts/active')
        alerts.value = await alertsResponse.json()
        
        // Load agents to get IP addresses
        const agentsResponse = await fetch('/api/agents')
        agents.value = await agentsResponse.json()
      } catch (error) {
        console.error('Error loading dashboard data:', error)
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
    
    const executeStepCommand = async (command) => {
      if (!selectedAlert.value || !command.trim()) return
      
      executingCommand.value = command
      isExecutingCommand.value = true
      
      // Find the step index
      const stepIndex = emergencyKnowledge.value.steps.findIndex(s => s.linuxCommand === command)
      
      try {
        const response = await fetch(`/api/agents/${selectedAlert.value.agentId}/execute`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ command: command })
        })
        
        if (response.ok) {
          const result = await response.json()
          stepResults.value[stepIndex] = result
          stepResultTab.value[stepIndex] = 'output' // Default to output tab
        } else {
          stepResults.value[stepIndex] = {
            error: `HTTP ${response.status}: ${response.statusText}`,
            timestamp: Date.now(),
            exitCode: -1
          }
          stepResultTab.value[stepIndex] = 'error'
        }
      } catch (error) {
        stepResults.value[stepIndex] = {
          error: `网络错误: ${error.message}`,
          timestamp: Date.now(),
          exitCode: -1
        }
        stepResultTab.value[stepIndex] = 'error'
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
      filteredAlerts,
      paginatedMetrics,
      
      // Methods
      getAgentIp,
      getMetricTypeName,
      getSeverityText,
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
      clearStepResult // Add clearStepResult to returned values
    }
  }
}
</script>

<style scoped>
/* 移除原有的样式定义，使用全局统一的样式 */
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
