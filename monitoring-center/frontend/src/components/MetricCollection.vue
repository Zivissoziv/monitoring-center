<template>
  <div class="metric-collection">
    
    
    <!-- Filter and View Controls -->
    <el-card class="control-panel" shadow="hover">
      <el-form :inline="true" label-width="90px">
        <el-form-item label="指标类型">
          <el-select v-model="selectedMetricType" @change="filterMetrics" placeholder="选择指标类型" style="width: 150px">
            <el-option label="全部指标" value="ALL" />
            <el-option v-for="definition in metricDefinitions" :key="definition.metricName" :label="definition.displayName" :value="definition.metricName" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="代理">
          <el-select v-model="selectedAgent" @change="filterMetrics" placeholder="选择代理" style="width: 150px">
            <el-option label="全部代理" value="ALL" />
            <el-option v-for="agentId in uniqueAgentIds" :key="agentId" :label="`代理 ${agentId}`" :value="agentId" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="x"
            @change="filterMetrics"
            style="width: 380px"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button-group>
            <el-button type="primary" @click="setQuickRange('1h')" size="small">最近1小时</el-button>
            <el-button type="primary" @click="setQuickRange('24h')" size="small">最近24小时</el-button>
            <el-button type="primary" @click="setQuickRange('7d')" size="small">最近7天</el-button>
            <el-button type="info" @click="clearTimeRange" size="small">清除时间</el-button>
          </el-button-group>
        </el-form-item>
      </el-form>
      
      <div class="view-toggle">
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
    </el-card>
    
    <!-- Table View -->
    <el-card v-if="viewMode === 'table'" class="metric-view" shadow="hover" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>监控数据表格</span>
        </div>
      </template>
      <el-table :data="paginatedMetrics" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="agentId" label="代理ID" width="100" />
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
      
      <div class="pagination-controls">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="totalMetrics"
          layout="prev, pager, next, jumper, ->, total"
          background
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
    
    <!-- Chart View -->
    <el-card v-if="viewMode === 'chart'" class="metric-view" shadow="hover" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>监控数据图表</span>
        </div>
      </template>
      <div v-if="filteredMetrics.length === 0">
        <el-empty description="暂无符合条件的监控数据" />
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
import { TrendCharts, Grid } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { Chart } from 'chart.js/auto'

export default {
  name: 'MetricCollection',
  components: {
    TrendCharts,
    Grid
  },
  data() {
    return {
      metrics: [],
      filteredMetrics: [],
      metricDefinitions: [],
      loading: false,
      selectedMetricType: 'ALL',
      selectedAgent: 'ALL',
      timeRange: null,
      viewMode: 'table',
      chartInstances: [],
      currentPage: 1,
      pageSize: 10,
      totalMetrics: 0
    }
  },
  computed: {
    uniqueAgentIds() {
      const agentIds = [...new Set(this.metrics.map(m => m.agentId))]
      return agentIds.sort((a, b) => a - b)
    },
    paginatedMetrics() {
      // This will now be populated from backend pagination
      return this.filteredMetrics
    },
    individualCharts() {
      // Prepare data for individual charts
      const charts = []
      
      if (this.selectedMetricType === 'ALL') {
        // Create separate charts for CPU and Memory
        const cpuMetrics = this.filteredMetrics.filter(m => m.metricType === 'CPU')
        const memoryMetrics = this.filteredMetrics.filter(m => m.metricType === 'MEMORY')
        
        if (cpuMetrics.length > 0) {
          charts.push({
            type: 'CPU',
            data: cpuMetrics,
            title: 'CPU使用率趋势'
          })
        }
        
        if (memoryMetrics.length > 0) {
          charts.push({
            type: 'MEMORY',
            data: memoryMetrics,
            title: '内存使用率趋势'
          })
        }
      } else {
        // Group by agent for the selected metric type
        const agentIds = [...new Set(this.filteredMetrics.map(m => m.agentId))]
        agentIds.forEach(agentId => {
          const agentMetrics = this.filteredMetrics.filter(m => m.agentId === agentId)
          if (agentMetrics.length > 0) {
            charts.push({
              type: this.selectedMetricType,
              agentId: agentId,
              data: agentMetrics,
              title: `代理${agentId} - ${this.getMetricTypeName(this.selectedMetricType)}趋势`
            })
          }
        })
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
    this.loadMetricDefinitions()
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
        const page = this.currentPage - 1 // Backend uses 0-based indexing
        const size = this.pageSize
        
        let url = ''
        const hasTimeRange = this.timeRange && this.timeRange.length === 2
        
        // Build URL based on filters
        if (this.selectedAgent !== 'ALL') {
          // Agent-specific queries
          if (this.selectedMetricType !== 'ALL') {
            // Agent + MetricType + optional TimeRange
            if (hasTimeRange) {
              url = `/api/metrics/agent/${this.selectedAgent}/type/${this.selectedMetricType}/timerange/paginated?startTime=${this.timeRange[0]}&endTime=${this.timeRange[1]}&page=${page}&size=${size}`
            } else {
              url = `/api/metrics/agent/${this.selectedAgent}/type/${this.selectedMetricType}/paginated?page=${page}&size=${size}`
            }
          } else {
            // Agent only + optional TimeRange
            if (hasTimeRange) {
              url = `/api/metrics/agent/${this.selectedAgent}/timerange/paginated?startTime=${this.timeRange[0]}&endTime=${this.timeRange[1]}&page=${page}&size=${size}`
            } else {
              url = `/api/metrics/agent/${this.selectedAgent}/paginated?page=${page}&size=${size}`
            }
          }
        } else {
          // All agents
          if (this.selectedMetricType !== 'ALL') {
            // MetricType filter without agent - need to load all and filter on frontend
            const response = await fetch('/api/metrics')
            const allMetrics = await response.json()
            let filtered = allMetrics.filter(m => m.metricType === this.selectedMetricType)
            
            // Apply time range filter if set
            if (hasTimeRange) {
              filtered = filtered.filter(m => 
                m.timestamp >= parseInt(this.timeRange[0]) && 
                m.timestamp <= parseInt(this.timeRange[1])
              )
            }
            
            this.metrics = filtered
            this.totalMetrics = filtered.length
            
            // Manual pagination for frontend filtering
            const startIndex = page * size
            const endIndex = startIndex + size
            this.filteredMetrics = filtered.slice(startIndex, endIndex)
            this.filteredMetrics.sort((a, b) => b.timestamp - a.timestamp)
            return
          } else {
            // All metrics - time range not supported for global query without agent
            // Just use pagination
            url = `/api/metrics/paginated?page=${page}&size=${size}`
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
        
        // Sort by timestamp descending (newest first)
        this.filteredMetrics.sort((a, b) => b.timestamp - a.timestamp)
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
      this.currentPage = 1 // Reset to first page when filters change
      this.loadPaginatedData()
    },
    
    handlePageChange(newPage) {
      this.currentPage = newPage
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
      
      this.timeRange = [startTime, now]
      this.filterMetrics()
    },
    
    clearTimeRange() {
      this.timeRange = null
      this.filterMetrics()
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
              
              // Prepare data for chart - limit to 100 data points for performance
              const maxDataPoints = 100
              let data = chartConfig.data.slice().reverse() // Reverse to show oldest to newest
              
              // Sample data if there are too many points
              if (data.length > maxDataPoints) {
                const step = Math.ceil(data.length / maxDataPoints)
                data = data.filter((_, i) => i % step === 0)
              }
              
              const labels = data.map(m => this.formatTimeShort(m.timestamp))
              const values = data.map(m => m.value)
              
              const chartInstance = new Chart(ctx, {
                type: 'line',
                data: {
                  labels: labels,
                  datasets: [{
                    label: chartConfig.title,
                    data: values,
                    borderColor: chartConfig.type === 'CPU' ? '#c62828' : '#1976d2',
                    backgroundColor: chartConfig.type === 'CPU' ? 'rgba(198, 40, 40, 0.1)' : 'rgba(25, 118, 210, 0.1)',
                    borderWidth: 2,
                    tension: 0.4,
                    pointRadius: data.length > 50 ? 0 : 2,
                    pointHoverRadius: 4
                  }]
                },
                options: {
                  responsive: true,
                  maintainAspectRatio: false,
                  plugins: {
                    title: {
                      display: true,
                      text: chartConfig.title,
                      font: {
                        size: 16,
                        weight: 'bold'
                      },
                      color: '#1976d2'
                    },
                    legend: {
                      display: true,
                      position: 'top',
                      labels: {
                        color: '#000000',
                        font: {
                          size: 12,
                          weight: '600'
                        }
                      }
                    },
                    tooltip: {
                      mode: 'index',
                      intersect: false,
                      backgroundColor: 'rgba(0, 0, 0, 0.8)',
                      titleColor: '#ffffff',
                      bodyColor: '#ffffff',
                      borderColor: '#1976d2',
                      borderWidth: 1
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
                        maxTicksLimit: 15,
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
    
    formatTime(timestamp) {
      return new Date(timestamp).toLocaleString('zh-CN')
    },
    
    formatTimeShort(timestamp) {
      const date = new Date(timestamp)
      return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
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

.pagination-controls {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.chart-item {
  background: #f9f9f9;
  border-radius: 8px;
  padding: 15px;
  min-height: 350px;
}

.chart-container {
  position: relative;
  height: 320px;
  width: 100%;
}

.metric-value {
  font-weight: 600;
  color: #1976d2;
  font-size: 15px;
}
</style>