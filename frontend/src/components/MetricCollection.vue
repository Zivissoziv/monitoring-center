<template>
  <div class="metric-collection">
    
    <!-- Search and Filter Bar -->
    <el-card shadow="hover" style="margin-bottom: 20px;">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="所属应用">
          <el-select v-model="searchForm.appCode" placeholder="全部" clearable style="width: 150px">
            <el-option 
              v-for="app in apps" 
              :key="app.appCode" 
              :label="`${app.appCode} - ${app.appName}`" 
              :value="app.appCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="指标类型">
          <el-select v-model="searchForm.metricType" placeholder="全部" clearable style="width: 150px">
            <el-option 
              v-for="definition in metricDefinitions" 
              :key="definition.metricName" 
              :label="definition.displayName" 
              :value="definition.metricName"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="代理">
          <el-select v-model="searchForm.agentId" placeholder="全部" clearable style="width: 200px">
            <el-option 
              v-for="agent in sortedAgents" 
              :key="agent.id" 
              :label="agent.name" 
              :value="agent.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="x"
            style="width: 380px"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :icon="Search">查询</el-button>
          <el-button @click="handleReset" :icon="Refresh">重置</el-button>
        </el-form-item>
      </el-form>
      
      
    </el-card>
    
    <!-- Table View -->
    <el-card v-if="viewMode === 'table'" class="metric-view" shadow="hover" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>监控数据表格</span>
          <el-radio-group v-model="viewMode" size="default">
            <el-radio-button label="table">
              <el-icon><Grid /></el-icon>
              <span>表格视图</span>
            </el-radio-button>
            <el-radio-button label="chart">
              <el-icon><TrendCharts /></el-icon>
              <span>图表视图</span>
            </el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table :data="paginatedMetrics" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="所属应用" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.appCode" type="primary" size="small">
              {{ getAppName(scope.row.appCode) }}
            </el-tag>
            <span v-else style="color: #909399">无</span>
          </template>
        </el-table-column>
        <el-table-column label="代理" width="150">
          <template #default="scope">
            {{ getAgentName(scope.row.agentId) }}
          </template>
        </el-table-column>
        <el-table-column label="指标类型" width="150">
          <template #default="scope">
            <el-tag :type="scope.row.metricType === 'CPU' ? 'danger' : 'primary'" size="small">
              {{ getMetricTypeName(scope.row.metricType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="数值" width="150">
          <template #default="scope">
            <span class="metric-value">{{ formatValue(scope.row.value, scope.row.metricType) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时间戳" min-width="180">
          <template #default="scope">
            {{ formatTime(scope.row.timestamp) }}
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无符合条件的监控数据" />
        </template>
      </el-table>
      
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalMetrics"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    
    <!-- Chart View -->
    <el-card v-if="viewMode === 'chart'" class="metric-view" shadow="hover" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>监控数据图表</span>
          <el-radio-group v-model="viewMode" size="default">
            <el-radio-button label="table">
              <el-icon><Grid /></el-icon>
              <span>表格视图</span>
            </el-radio-button>
            <el-radio-button label="chart">
              <el-icon><TrendCharts /></el-icon>
              <span>图表视图</span>
            </el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div v-if="filteredMetrics.length === 0">
        <el-empty description="暂无符合条件的监控数据" />
      </div>
      <div v-else-if="individualCharts.length === 0">
        <el-empty description="当前指标不支持图表显示（仅数值型指标支持图表视图）" />
      </div>
      <div v-else class="charts-grid">
        <div 
          v-for="(chartData, index) in individualCharts" 
          :key="index" 
          class="chart-item"
        >
          <div class="chart-container">
            <canvas :ref="`chart-${index}`"></canvas>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { TrendCharts, Grid, Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { Chart } from 'chart.js/auto'

export default {
  name: 'MetricCollection',
  components: {
    TrendCharts,
    Grid,
    Search,
    Refresh
  },
  data() {
    return {
      metrics: [],
      filteredMetrics: [],
      metricDefinitions: [],
      agents: [],
      apps: [],
      loading: false,
      searchForm: {
        appCode: '',
        metricType: '',
        agentId: '',
        timeRange: null
      },
      pagination: {
        currentPage: 1,
        pageSize: 10
      },
      viewMode: 'table',
      chartInstances: [],
      totalMetrics: 0
    }
  },
  computed: {
    sortedAgents() {
      return this.agents.sort((a, b) => a.name.localeCompare(b.name))
    },
    paginatedMetrics() {
      // This will now be populated from backend pagination
      return this.filteredMetrics
    },
    individualCharts() {
      // Prepare data for individual charts - only for NUMERIC metrics
      const charts = []
      
      // Helper function to check if a metric is numeric
      const isNumericMetric = (metricName) => {
        const definition = this.metricDefinitions.find(d => d.metricName === metricName)
        return definition && definition.metricType === 'NUMERIC'
      }
      
      if (this.searchForm.metricType === '') {
        // Create separate charts for each numeric metric type
        const metricTypes = [...new Set(this.filteredMetrics.map(m => m.metricType))]
        
        metricTypes.forEach(metricType => {
          // Only create chart if it's a numeric metric
          if (isNumericMetric(metricType)) {
            const typeMetrics = this.filteredMetrics.filter(m => m.metricType === metricType)
            
            if (typeMetrics.length > 0) {
              charts.push({
                type: metricType,
                data: typeMetrics,
                title: `${this.getMetricTypeName(metricType)}趋势`
              })
            }
          }
        })
      } else {
        // Only show chart if the selected metric type is numeric
        if (isNumericMetric(this.searchForm.metricType)) {
          // Group by agent for the selected metric type
          const agentIds = [...new Set(this.filteredMetrics.map(m => m.agentId))]
          agentIds.forEach(agentId => {
            const agentMetrics = this.filteredMetrics.filter(m => 
              (!this.searchForm.agentId || m.agentId === this.searchForm.agentId) &&
              (!this.searchForm.metricType || m.metricType === this.searchForm.metricType)
            )
            if (agentMetrics.length > 0) {
              const metricType = this.searchForm.metricType || agentMetrics[0].metricType
              charts.push({
                type: metricType,
                agentId: agentId,
                data: agentMetrics,
                title: `${this.getAgentName(agentId)} - ${this.getMetricTypeName(metricType)}趋势`
              })
            }
          })
        }
      }
      
      return charts
    }
  },
  watch: {
    viewMode(newMode) {
      if (newMode === 'chart') {
        this.$nextTick(() => {
          this.renderIndividualCharts()
        })
      }
    },
    filteredMetrics() {
      if (this.viewMode === 'chart') {
        this.$nextTick(() => {
          this.renderIndividualCharts()
        })
      }
    }
  },
  mounted() {
    this.loadAgents()
    this.loadMetricDefinitions()
    this.loadApps()
    this.loadAllData()
    // Removed auto-refresh
  },
  beforeUnmount() {
    // Clean up chart instances
    this.chartInstances.forEach(chart => {
      if (chart) {
        chart.destroy()
      }
    })
  },
  methods: {
    async loadAgents() {
      try {
        const response = await fetch('/api/agents')
        this.agents = await response.json()
      } catch (error) {
        console.error('Error loading agents:', error)
      }
    },
    
    async loadMetricDefinitions() {
      try {
        const response = await fetch('/api/metric-definitions/enabled')
        this.metricDefinitions = await response.json()
      } catch (error) {
        console.error('Error loading metric definitions:', error)
      }
    },
    
    async loadAllData() {
      try {
        // Load paginated metrics
        await this.loadPaginatedData()
        this.filterMetrics()
      } catch (error) {
        console.error('Error loading data:', error)
      }
    },
    
    async loadPaginatedData() {
      this.loading = true
      try {
        const page = this.pagination.currentPage - 1 // Backend uses 0-based indexing
        const size = this.pagination.pageSize
        
        let url = ''
        const hasTimeRange = this.searchForm.timeRange && this.searchForm.timeRange.length === 2
        
        // Build URL based on filters
        if (this.searchForm.agentId) {
          // Agent-specific queries
          if (this.searchForm.metricType) {
            // Agent + MetricType + optional TimeRange
            if (hasTimeRange) {
              const startTime = this.formatTimeForBackend(this.searchForm.timeRange[0])
              const endTime = this.formatTimeForBackend(this.searchForm.timeRange[1])
              url = `/api/metrics/agent/${this.searchForm.agentId}/type/${this.searchForm.metricType}/timerange?startTime=${encodeURIComponent(startTime)}&endTime=${encodeURIComponent(endTime)}&page=${page}&size=${size}`
            } else {
              url = `/api/metrics/agent/${this.searchForm.agentId}/type/${this.searchForm.metricType}?page=${page}&size=${size}`
            }
          } else {
            // Agent only + optional TimeRange
            if (hasTimeRange) {
              const startTime = this.formatTimeForBackend(this.searchForm.timeRange[0])
              const endTime = this.formatTimeForBackend(this.searchForm.timeRange[1])
              url = `/api/metrics/agent/${this.searchForm.agentId}/timerange?startTime=${encodeURIComponent(startTime)}&endTime=${encodeURIComponent(endTime)}&page=${page}&size=${size}`
            } else {
              url = `/api/metrics/agent/${this.searchForm.agentId}?page=${page}&size=${size}`
            }
          }
        } else {
          // All agents
          if (this.searchForm.metricType) {
            // MetricType filter without agent - need to load all and filter on frontend
            const response = await fetch('/api/metrics')
            const allMetrics = await response.json()
            let filtered = allMetrics.filter(m => m.metricType === this.searchForm.metricType)
            
            // Apply time range filter if set
            if (hasTimeRange) {
              const startMs = parseInt(this.searchForm.timeRange[0])
              const endMs = parseInt(this.searchForm.timeRange[1])
              filtered = filtered.filter(m => {
                const ts = this.parseTimestamp(m.timestamp)
                return ts >= startMs && ts <= endMs
              })
            }
            
            // Apply appCode filter if set
            if (this.searchForm.appCode) {
              filtered = filtered.filter(m => m.appCode === this.searchForm.appCode)
            }
            
            this.metrics = filtered
            this.totalMetrics = filtered.length
            
            // Manual pagination for frontend filtering
            const startIndex = page * size
            const endIndex = startIndex + size
            this.filteredMetrics = filtered.slice(startIndex, endIndex)
            this.filteredMetrics.sort((a, b) => this.parseTimestamp(b.timestamp) - this.parseTimestamp(a.timestamp))
            return
          } else {
            // All metrics - time range not supported for global query without agent
            // Just use pagination
            url = `/api/metrics?page=${page}&size=${size}`
          }
        }
        
        const response = await fetch(url)
        const data = await response.json()
        
        // Handle paginated response
        if (data.content) {
          this.filteredMetrics = data.content
          this.totalMetrics = data.totalElements
        } else {
          // Fallback if pagination isn't supported
          this.filteredMetrics = data
          this.totalMetrics = data.length
        }
        
        // Apply appCode filter if set (frontend filtering)
        if (this.searchForm.appCode) {
          this.filteredMetrics = this.filteredMetrics.filter(m => m.appCode === this.searchForm.appCode)
          this.totalMetrics = this.filteredMetrics.length
        }
        
        // Sort by timestamp descending (newest first)
        this.filteredMetrics.sort((a, b) => this.parseTimestamp(b.timestamp) - this.parseTimestamp(a.timestamp))
      } catch (error) {
        console.error('Error loading paginated data:', error)
        ElMessage.error('加载数据失败')
      } finally {
        this.loading = false
      }
    },
    
    filterMetrics() {
      // Filtering is now mostly handled by backend pagination
      // But we still need to handle some frontend filtering for edge cases
      this.pagination.currentPage = 1 // Reset to first page when filters change
      this.loadPaginatedData()
    },
    
    handleSearch() {
      this.pagination.currentPage = 1
      this.loadPaginatedData()
    },
    
    handleReset() {
      this.searchForm = {
        appCode: '',
        metricType: '',
        agentId: '',
        timeRange: null
      }
      this.pagination.currentPage = 1
      this.loadPaginatedData()
    },
    
    handlePageChange(newPage) {
      this.pagination.currentPage = newPage
      this.loadPaginatedData()
    },
    
    handleSizeChange(val) {
      this.pagination.pageSize = val
      this.pagination.currentPage = 1
      this.loadPaginatedData()
    },
    
    handleCurrentChange(val) {
      this.pagination.currentPage = val
      this.loadPaginatedData()
    },
    
    setQuickRange(range) {
      const now = Date.now()
      let startTime
      
      switch(range) {
        case '1h':
          startTime = now - (60 * 60 * 1000) // 1 hour ago
          break
        case '24h':
          startTime = now - (24 * 60 * 60 * 1000) // 24 hours ago
          break
        case '7d':
          startTime = now - (7 * 24 * 60 * 60 * 1000) // 7 days ago
          break
        default:
          return
      }
      
      this.searchForm.timeRange = [startTime, now]
      this.handleSearch()
    },
    
    renderIndividualCharts() {
      // Clean up existing chart instances
      this.chartInstances.forEach(chart => {
        if (chart) {
          chart.destroy()
        }
      })
      this.chartInstances = []
      
      // Render each individual chart
      this.individualCharts.forEach((chartConfig, index) => {
        this.$nextTick(() => {
          const canvasRef = this.$refs[`chart-${index}`]
          if (canvasRef && canvasRef[0]) {
            try {
              const ctx = canvasRef[0].getContext('2d')
              
              // Get metric definition for unit info
              const metricDef = this.metricDefinitions.find(d => d.metricName === chartConfig.type)
              const unit = metricDef ? metricDef.unit : ''
              
              // Prepare data for chart - limit to 200 data points for better visibility
              const maxDataPoints = 200
              let data = chartConfig.data.slice().reverse() // Reverse to show oldest to newest
              
              // Sample data if there are too many points (use smart sampling)
              if (data.length > maxDataPoints) {
                const step = Math.floor(data.length / maxDataPoints)
                data = data.filter((_, i) => i % step === 0).slice(0, maxDataPoints)
              }
              
              const labels = data.map(m => this.formatTimeShort(m.timestamp))
              const values = data.map(m => m.value)
              
              // Dynamic color based on metric type
              const getMetricColor = (type) => {
                const colors = {
                  'CPU': { border: '#FF6B6B', bg: 'rgba(255, 107, 107, 0.15)' },
                  'MEMORY': { border: '#4ECDC4', bg: 'rgba(78, 205, 196, 0.15)' },
                  'DISK': { border: '#95E1D3', bg: 'rgba(149, 225, 211, 0.15)' },
                  'NETWORK': { border: '#F38181', bg: 'rgba(243, 129, 129, 0.15)' }
                }
                return colors[type] || { border: '#5B8FF9', bg: 'rgba(91, 143, 249, 0.15)' }
              }
              
              const colors = getMetricColor(chartConfig.type)
              
              const chartInstance = new Chart(ctx, {
                type: 'line',
                data: {
                  labels: labels,
                  datasets: [{
                    label: chartConfig.title,
                    data: values,
                    borderColor: colors.border,
                    backgroundColor: colors.bg,
                    borderWidth: 2.5,
                    tension: 0.35,
                    pointRadius: data.length > 100 ? 2 : 3,
                    pointHoverRadius: 6,
                    pointBackgroundColor: colors.border,
                    pointBorderColor: '#fff',
                    pointBorderWidth: 1.5,
                    pointHoverBorderWidth: 2,
                    fill: true
                  }]
                },
                options: {
                  responsive: true,
                  maintainAspectRatio: false,
                  interaction: {
                    mode: 'index',
                    intersect: false
                  },
                  plugins: {
                    title: {
                      display: true,
                      text: chartConfig.title,
                      font: {
                        size: 18,
                        weight: 'bold',
                        family: "'Helvetica Neue', 'Arial', sans-serif"
                      },
                      color: '#2C3E50',
                      padding: {
                        top: 10,
                        bottom: 20
                      }
                    },
                    legend: {
                      display: true,
                      position: 'top',
                      align: 'end',
                      labels: {
                        color: '#2C3E50',
                        font: {
                          size: 13,
                          weight: '600'
                        },
                        padding: 15,
                        usePointStyle: true,
                        pointStyle: 'circle'
                      }
                    },
                    tooltip: {
                      enabled: true,
                      mode: 'index',
                      intersect: false,
                      backgroundColor: 'rgba(44, 62, 80, 0.95)',
                      titleColor: '#ECF0F1',
                      bodyColor: '#ECF0F1',
                      borderColor: colors.border,
                      borderWidth: 2,
                      padding: 12,
                      titleFont: {
                        size: 14,
                        weight: 'bold'
                      },
                      bodyFont: {
                        size: 13
                      },
                      displayColors: true,
                      callbacks: {
                        label: function(context) {
                          let label = context.dataset.label || ''
                          if (label) {
                            label += ': '
                          }
                          if (context.parsed.y !== null) {
                            label += context.parsed.y.toFixed(2) + (unit ? ' ' + unit : '')
                          }
                          return label
                        }
                      }
                    }
                  },
                  scales: {
                    y: {
                      beginAtZero: true,
                      ticks: {
                        callback: (value) => {
                          return value.toFixed(1) + (unit ? ' ' + unit : '')
                        },
                        color: '#34495E',
                        font: {
                          size: 12,
                          weight: '500'
                        },
                        padding: 8
                      },
                      grid: {
                        color: 'rgba(149, 165, 166, 0.2)',
                        lineWidth: 1,
                        drawBorder: false
                      },
                      border: {
                        display: false
                      }
                    },
                    x: {
                      ticks: {
                        color: '#34495E',
                        font: {
                          size: 11,
                          weight: '500'
                        },
                        maxTicksLimit: 20,
                        maxRotation: 45,
                        minRotation: 30,
                        autoSkip: true,
                        autoSkipPadding: 10
                      },
                      grid: {
                        color: 'rgba(149, 165, 166, 0.15)',
                        lineWidth: 1,
                        drawBorder: false
                      },
                      border: {
                        display: false
                      }
                    }
                  },
                  elements: {
                    line: {
                      borderJoinStyle: 'round'
                    }
                  }
                }
              })
              
              this.chartInstances.push(chartInstance)
            } catch (error) {
              console.error('Error rendering chart:', error)
            }
          }
        })
      })
    },
    
    getMetricTypeName(type) {
      const definition = this.metricDefinitions.find(d => d.metricName === type)
      return definition ? definition.displayName : type
    },
    
    formatValue(value, type) {
      const definition = this.metricDefinitions.find(d => d.metricName === type)
      const unit = definition ? definition.unit : ''
      return value.toFixed(2) + (unit ? ' ' + unit : '')
    },
    
    // Parse timestamp - handles both ISO string and milliseconds
    parseTimestamp(timestamp) {
      if (typeof timestamp === 'string') {
        return new Date(timestamp).getTime()
      }
      return timestamp
    },
    
    // Format milliseconds to ISO string for backend API
    formatTimeForBackend(ms) {
      return new Date(ms).toISOString().slice(0, 19)
    },
    
    formatTime(timestamp) {
      const date = typeof timestamp === 'string' ? new Date(timestamp) : new Date(timestamp)
      return date.toLocaleString('zh-CN')
    },
    
    formatTimeShort(timestamp) {
      const date = typeof timestamp === 'string' ? new Date(timestamp) : new Date(timestamp)
      return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    },
    
    getAgentName(agentId) {
      const agent = this.agents.find(a => a.id === agentId)
      return agent ? agent.name : agentId
    },
    
    async loadApps() {
      try {
        const response = await fetch('/api/apps/enabled')
        const result = await response.json()
        if (result.success) {
          this.apps = result.data || []
        }
      } catch (error) {
        console.error('Error loading apps:', error)
      }
    },
    
    getAppName(appCode) {
      const app = this.apps.find(a => a.appCode === appCode)
      return app ? appCode : appCode
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

.metric-collection {
  padding: 20px;
}

.el-page-header {
  margin-bottom: 24px;
}

.control-panel {
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.view-toggle {
  display: flex;
  justify-content: flex-end;
}

.metric-view {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 16px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(600px, 1fr));
  gap: 24px;
  margin-top: 20px;
}

.chart-item {
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  border-radius: 12px;
  padding: 20px;
  min-height: 400px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.chart-item:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.chart-container {
  position: relative;
  height: 360px;
  width: 100%;
}

.metric-value {
  font-weight: 600;
  color: #1976d2;
  font-size: 15px;
}
</style>