<template>
  <div class="metric-collection">
    <div class="section-header">
      <h2><span class="icon">📈</span> 监控指标</h2>
    </div>
    
    <!-- Filter and View Controls -->
    <div class="control-panel card">
      <div class="filter-section">
        <div class="filter-group">
          <label>指标类型:</label>
          <select v-model="selectedMetricType" @change="filterMetrics" class="filter-select">
            <option value="ALL">全部指标</option>
            <option value="CPU">CPU使用率</option>
            <option value="MEMORY">内存使用率</option>
          </select>
        </div>
        
        <div class="filter-group">
          <label>代理:</label>
          <select v-model="selectedAgent" @change="filterMetrics" class="filter-select">
            <option value="ALL">全部代理</option>
            <option v-for="agentId in uniqueAgentIds" :key="agentId" :value="agentId">
              代理 {{ agentId }}
            </option>
          </select>
        </div>
        
        <div class="filter-group">
          <label>时间范围:</label>
          <select v-model="timeRange" @change="filterMetrics" class="filter-select" disabled>
            <option value="10">最近10条</option>
            <option value="20">最近20条</option>
            <option value="50">最近50条</option>
            <option value="100">最近100条</option>
            <option value="200">最近200条</option>
            <option value="500">最近500条</option>
          </select>
        </div>
      </div>
      
      <div class="view-toggle">
        <button 
          :class="['toggle-btn', { active: viewMode === 'table' }]" 
          @click="viewMode = 'table'"
        >
          📋 表格视图
        </button>
        <button 
          :class="['toggle-btn', { active: viewMode === 'chart' }]" 
          @click="viewMode = 'chart'"
        >
          📊 图表视图
        </button>
      </div>
    </div>
    
    <!-- Table View -->
    <div v-if="viewMode === 'table'" class="metric-view card">
      <h3>监控数据表格</h3>
      <div v-if="paginatedMetrics.length === 0" class="no-data-message">
        <p>📊 暂无符合条件的监控数据</p>
      </div>
      <div v-else class="table-container">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>代理ID</th>
              <th>指标类型</th>
              <th>数值</th>
              <th>时间戳</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="metric in paginatedMetrics" :key="metric.id">
              <td>{{ metric.id }}</td>
              <td>{{ metric.agentId }}</td>
              <td><span :class="'metric-type metric-' + metric.metricType.toLowerCase()">{{ getMetricTypeName(metric.metricType) }}</span></td>
              <td><span class="metric-value">{{ formatValue(metric.value, metric.metricType) }}</span></td>
              <td>{{ formatTime(metric.timestamp) }}</td>
            </tr>
          </tbody>
        </table>
        
        <!-- Pagination Controls -->
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
      </div>
    </div>
    
    <!-- Chart View -->
    <div v-if="viewMode === 'chart'" class="metric-view card">
      <h3>监控数据图表</h3>
      <div v-if="filteredMetrics.length === 0" class="no-data-message">
        <p>📊 暂无符合条件的监控数据</p>
      </div>
      <div v-else class="charts-grid">
        <!-- Individual charts for each metric type and agent combination -->
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
    </div>
  </div>
</template>

<script>
import { Chart } from 'chart.js/auto'

export default {
  name: 'MetricCollection',
  data() {
    return {
      metrics: [],
      filteredMetrics: [],
      selectedMetricType: 'ALL',
      selectedAgent: 'ALL',
      timeRange: '100',
      viewMode: 'table', // 'table' or 'chart'
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
      try {
        const page = this.currentPage - 1 // Backend uses 0-based indexing
        const size = this.pageSize
        
        let url = `/api/metrics/paginated?page=${page}&size=${size}`
        
        // Add filters if applicable
        if (this.selectedAgent !== 'ALL') {
          url = `/api/metrics/agent/${this.selectedAgent}/paginated?page=${page}&size=${size}`
        }
        
        if (this.selectedMetricType !== 'ALL' && this.selectedAgent !== 'ALL') {
          url = `/api/metrics/agent/${this.selectedAgent}/type/${this.selectedMetricType}/paginated?page=${page}&size=${size}`
        } else if (this.selectedMetricType !== 'ALL' && this.selectedAgent === 'ALL') {
          // For metric type filter without agent, we need to load all and filter on frontend
          // This is a limitation of our current backend API
          const response = await fetch('/api/metrics')
          const allMetrics = await response.json()
          this.metrics = allMetrics.filter(m => m.metricType === this.selectedMetricType)
          this.totalMetrics = this.metrics.length
          return
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
      const typeMap = {
        'CPU': 'CPU使用率',
        'MEMORY': '内存使用率'
      }
      return typeMap[type] || type
    },
    
    formatValue(value, type) {
      if (type === 'CPU' || type === 'MEMORY') {
        return value.toFixed(2) + '%'
      }
      return value.toFixed(2)
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
.control-panel {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 20px;
}

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

.view-toggle {
  display: flex;
  gap: 10px;
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

.pagination-controls {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-top: 20px;
}

.chart-item {
  background: #f9f9f9;
  border-radius: 8px;
  padding: 15px;
}

.chart-container {
  position: relative;
  height: 300px;
  width: 100%;
}

.metric-view table {
  width: 100%;
  border-collapse: collapse;
}

.metric-view th, .metric-view td {
  border: 1px solid #e0e0e0;
  padding: 8px;
  text-align: left;
  color: #1976d2;
}

.metric-view th {
  background-color: #e3f2fd;
  color: #1976d2;
}

.no-data-message {
  text-align: center;
  padding: 60px 20px;
  color: #9e9e9e;
  font-size: 16px;
  font-weight: 600;
}

.no-data-message p {
  margin: 0;
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
  font-size: 15px;
}

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

.table-container {
  overflow-x: auto;
}
</style>